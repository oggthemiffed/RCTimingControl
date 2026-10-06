import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { getAccessToken } from '@/lib/auth';

export type StompStatus = 'disconnected' | 'connecting' | 'connected' | 'error';

export function useStomp<T>(topic: string | null) {
  // Each frame is tagged with its topic so a previous topic's last frame is never returned
  // after the topic changes (e.g. a board moving on to the next race).
  const [frame, setFrame] = useState<{ topic: string; data: T } | null>(null);
  // The connection status is tagged the same way, so a new topic reads as connecting until its own client reports
  const [connection, setConnection] = useState<{ topic: string; status: StompStatus } | null>(null);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!topic) return;
    // A client being shut down can still report; only the current one changes the status or the data
    let active = true;
    const setStatus = (status: StompStatus) => {
      if (active) setConnection({ topic, status });
    };

    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${proto}//${window.location.host}/ws/timing`;

    // Anonymous spectators (L12 boards) connect without a token; the server only lets
    // them subscribe to a race's timing/state topics.
    const token = getAccessToken();
    const client = new Client({
      brokerURL: wsUrl,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5_000,
      onConnect: () => {
        setStatus('connected');
        client.subscribe(topic, (msg) => {
          try {
            if (active) setFrame({ topic, data: JSON.parse(msg.body) as T });
          } catch {
            // ignore malformed frame
          }
        });
      },
      onDisconnect: () => setStatus('disconnected'),
      onStompError: () => setStatus('error'),
      onWebSocketError: () => setStatus('error'),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      active = false;
      client.deactivate();
      clientRef.current = null;
      setConnection(null);
    };
  }, [topic]);

  const data = frame && frame.topic === topic ? frame.data : null;
  const status: StompStatus = !topic
    ? 'disconnected'
    : connection && connection.topic === topic
      ? connection.status
      : 'connecting';
  return { data, status };
}
