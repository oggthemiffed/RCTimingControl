import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import { getAccessToken } from '@/lib/auth';

export type StompStatus = 'disconnected' | 'connecting' | 'connected' | 'error';

export function useStomp<T>(topic: string | null) {
  const [data, setData] = useState<T | null>(null);
  const [status, setStatus] = useState<StompStatus>('disconnected');
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    if (!topic) {
      setStatus('disconnected');
      setData(null);
      return;
    }

    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${proto}//${window.location.host}/ws/timing`;

    const client = new Client({
      brokerURL: wsUrl,
      connectHeaders: {
        Authorization: `Bearer ${getAccessToken() ?? ''}`,
      },
      reconnectDelay: 5_000,
      onConnect: () => {
        setStatus('connected');
        client.subscribe(topic, (msg) => {
          try {
            setData(JSON.parse(msg.body) as T);
          } catch {
            // ignore malformed frame
          }
        });
      },
      onDisconnect: () => setStatus('disconnected'),
      onStompError: () => setStatus('error'),
      onWebSocketError: () => setStatus('error'),
    });

    setStatus('connecting');
    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setStatus('disconnected');
    };
  }, [topic]);

  return { data, status };
}
