import { useEffect, useState } from 'react';
import { subscribeTopic, type StompStatus } from '@/lib/stompConnection';

export type { StompStatus };

export function useStomp<T>(topic: string | null) {
  // Each frame is tagged with its topic so a previous topic's last frame is never returned
  // after the topic changes (e.g. a board moving on to the next race).
  const [frame, setFrame] = useState<{ topic: string; data: T } | null>(null);
  // The connection status is tagged the same way, so a new topic reads as connecting until it reports
  const [connection, setConnection] = useState<{ topic: string; status: StompStatus } | null>(null);

  useEffect(() => {
    if (!topic) return;
    // A listener that has been stopped can still be called once; only the current one changes anything
    let active = true;
    const stop = subscribeTopic(topic, {
      onFrame: (body) => {
        try {
          if (active) setFrame({ topic, data: JSON.parse(body) as T });
        } catch {
          // ignore malformed frame
        }
      },
      onStatus: (status) => {
        if (active) setConnection({ topic, status });
      },
    });

    return () => {
      active = false;
      stop();
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
