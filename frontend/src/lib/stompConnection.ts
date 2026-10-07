import { Client, type StompSubscription } from '@stomp/stompjs';
import { currentAccessToken } from '@/lib/auth';

export type StompStatus = 'disconnected' | 'connecting' | 'connected' | 'error';

interface Listener {
  onFrame: (body: string) => void;
  onStatus: (status: StompStatus) => void;
}

interface TopicEntry {
  listeners: Set<Listener>;
  subscription: StompSubscription | undefined;
}

// One WebSocket serves every topic the page listens to (the cockpit alone watches about eight). It opens
// with the first subscriber and closes with the last, so a page that listens to nothing holds no socket.
let client: Client | null = null;
let status: StompStatus = 'connecting';
const topics = new Map<string, TopicEntry>();

function setStatus(next: StompStatus): void {
  status = next;
  topics.forEach((entry) => entry.listeners.forEach((l) => l.onStatus(next)));
}

function subscribeToBroker(topic: string, entry: TopicEntry): void {
  entry.subscription = client?.subscribe(topic, (msg) => entry.listeners.forEach((l) => l.onFrame(msg.body)));
}

function openClient(): Client {
  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  // A client being shut down can still report; only the current one changes the status
  const current = (c: Client) => c === client;
  const created: Client = new Client({
    brokerURL: `${proto}//${window.location.host}/ws/timing`,
    reconnectDelay: 5_000,
    // Runs before every connect and reconnect, so a page left open past the 15-minute access token
    // reconnects with a renewed token instead of repeating one the server now refuses.
    // Anonymous spectators (boards) connect without a token; the server only lets them subscribe to a
    // race's timing and state topics.
    beforeConnect: async (c) => {
      const token = await currentAccessToken();
      c.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
    },
    // The broker forgets subscriptions when a connection drops, so every topic is subscribed again
    onConnect: () => {
      if (!current(created)) return;
      topics.forEach((entry, topic) => subscribeToBroker(topic, entry));
      setStatus('connected');
    },
    onDisconnect: () => current(created) && setStatus('disconnected'),
    onStompError: () => current(created) && setStatus('error'),
    onWebSocketError: () => current(created) && setStatus('error'),
  });
  return created;
}

/**
 * Listens to a topic on the shared connection. Each frame's body goes to {@code onFrame}, and the
 * connection's status to {@code onStatus} (at once, then on every change). Returns the function that
 * stops listening; the last one to stop closes the connection.
 */
export function subscribeTopic(topic: string, listener: Listener): () => void {
  let entry = topics.get(topic);
  if (!entry) {
    entry = { listeners: new Set(), subscription: undefined };
    topics.set(topic, entry);
  }
  entry.listeners.add(listener);

  if (!client) {
    status = 'connecting';
    client = openClient();
    client.activate();
  } else if (client.connected && !entry.subscription) {
    subscribeToBroker(topic, entry);
  }
  listener.onStatus(status);

  return () => {
    const current = topics.get(topic);
    if (!current) return;
    current.listeners.delete(listener);
    if (current.listeners.size > 0) return;
    try {
      current.subscription?.unsubscribe();
    } catch {
      // The connection dropped since it was subscribed: the broker has already forgotten it
    }
    topics.delete(topic);
    if (topics.size === 0 && client) {
      const closing = client;
      client = null;
      closing.deactivate();
    }
  };
}
