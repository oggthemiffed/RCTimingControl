import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';

interface FakeClient {
  config: {
    onConnect: () => void;
    onDisconnect: () => void;
    onStompError: () => void;
    onWebSocketError: () => void;
  };
  activate: ReturnType<typeof vi.fn>;
  deactivate: ReturnType<typeof vi.fn>;
  subscribe: ReturnType<typeof vi.fn>;
  connected: boolean;
  /** Frames for one topic, as the broker would send them */
  deliver: (body: string, topic?: string) => void;
}

const clients: FakeClient[] = [];

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn().mockImplementation(function (this: unknown, config: FakeClient['config']) {
    const handlers = new Map<string, (msg: { body: string }) => void>();
    const client: FakeClient = {
      config,
      connected: false,
      activate: vi.fn(),
      deactivate: vi.fn(),
      subscribe: vi.fn((topic: string, h: (msg: { body: string }) => void) => {
        handlers.set(topic, h);
        return { unsubscribe: vi.fn(() => handlers.delete(topic)) };
      }),
      // With no topic given, the only subscription gets it
      deliver: (body, topic) => [...(topic ? [handlers.get(topic)] : handlers.values())].forEach((h) => h?.({ body })),
    };
    clients.push(client);
    return client;
  }),
}));

const { currentAccessToken } = vi.hoisted(() => ({ currentAccessToken: vi.fn() }));
vi.mock('@/lib/auth', () => ({ currentAccessToken }));

import { useStomp } from './useStomp';

describe('useStomp', () => {
  beforeEach(() => {
    clients.length = 0;
    currentAccessToken.mockReset();
    currentAccessToken.mockResolvedValue(null);
  });

  it('asks for a current token before every connect, so a reconnect never reuses an expired one', async () => {
    currentAccessToken.mockResolvedValueOnce('first').mockResolvedValueOnce('second');
    renderHook(() => useStomp<{ n: number }>('/topic/a'));
    const config = clients[0].config as unknown as {
      beforeConnect: (c: { connectHeaders: Record<string, string> }) => Promise<void>;
    };
    const client = { connectHeaders: {} as Record<string, string> };

    await config.beforeConnect(client);
    expect(client.connectHeaders).toEqual({ Authorization: 'Bearer first' });
    await config.beforeConnect(client);
    expect(client.connectHeaders).toEqual({ Authorization: 'Bearer second' });
  });

  it('connects without a token when there is none (a spectator board)', async () => {
    renderHook(() => useStomp<{ n: number }>('/topic/a'));
    const config = clients[0].config as unknown as {
      beforeConnect: (c: { connectHeaders: Record<string, string> }) => Promise<void>;
    };
    const client = { connectHeaders: { Authorization: 'Bearer stale' } as Record<string, string> };

    await config.beforeConnect(client);

    expect(client.connectHeaders).toEqual({});
  });

  it('is disconnected with no topic and opens no connection', () => {
    const { result } = renderHook(() => useStomp<{ n: number }>(null));
    expect(result.current).toEqual({ data: null, status: 'disconnected' });
    expect(clients).toHaveLength(0);
  });

  it('connects, then returns each frame', () => {
    const { result } = renderHook(() => useStomp<{ n: number }>('/topic/a'));
    expect(result.current.status).toBe('connecting');
    expect(clients[0].activate).toHaveBeenCalled();

    act(() => clients[0].config.onConnect());
    expect(result.current.status).toBe('connected');
    expect(clients[0].subscribe).toHaveBeenCalledWith('/topic/a', expect.any(Function));

    act(() => clients[0].deliver('{"n":1}'));
    expect(result.current.data).toEqual({ n: 1 });

    act(() => clients[0].deliver('not json'));
    expect(result.current.data).toEqual({ n: 1 });

    act(() => clients[0].config.onWebSocketError());
    expect(result.current.status).toBe('error');
  });

  it('starts again on a new topic and ignores the old connection', () => {
    const { result, rerender } = renderHook(({ topic }) => useStomp<{ n: number }>(topic), {
      initialProps: { topic: '/topic/a' as string | null },
    });
    act(() => clients[0].config.onConnect());
    act(() => clients[0].deliver('{"n":1}'));

    rerender({ topic: '/topic/b' });
    expect(clients[0].deactivate).toHaveBeenCalled();
    expect(result.current).toEqual({ data: null, status: 'connecting' });

    // The old client reporting its shutdown late changes nothing
    act(() => clients[0].config.onDisconnect());
    expect(result.current.status).toBe('connecting');

    act(() => clients[1].config.onConnect());
    expect(result.current.status).toBe('connected');
  });

  it('is disconnected again when the topic goes away', () => {
    const { result, rerender } = renderHook(({ topic }) => useStomp<{ n: number }>(topic), {
      initialProps: { topic: '/topic/a' as string | null },
    });
    act(() => clients[0].config.onConnect());

    rerender({ topic: null });
    expect(result.current.status).toBe('disconnected');
    expect(clients[0].deactivate).toHaveBeenCalled();
  });

  it('shares one connection between topics, and a late topic subscribes at once when connected', () => {
    const a = renderHook(() => useStomp<{ n: number }>('/topic/a'));
    const b = renderHook(() => useStomp<{ n: number }>('/topic/b'));
    expect(clients).toHaveLength(1);

    clients[0].connected = true;
    act(() => clients[0].config.onConnect());
    expect(clients[0].subscribe).toHaveBeenCalledTimes(2);
    act(() => clients[0].deliver('{"n":1}', '/topic/a'));
    act(() => clients[0].deliver('{"n":2}', '/topic/b'));
    expect(a.result.current.data).toEqual({ n: 1 });
    expect(b.result.current.data).toEqual({ n: 2 });

    const c = renderHook(() => useStomp<{ n: number }>('/topic/c'));
    expect(clients).toHaveLength(1);
    expect(clients[0].subscribe).toHaveBeenCalledWith('/topic/c', expect.any(Function));
    expect(c.result.current.status).toBe('connected');
  });

  it('gives two listeners on one topic the same frames, with one subscription', () => {
    const first = renderHook(() => useStomp<{ n: number }>('/topic/a'));
    const second = renderHook(() => useStomp<{ n: number }>('/topic/a'));
    act(() => clients[0].config.onConnect());
    expect(clients[0].subscribe).toHaveBeenCalledTimes(1);

    act(() => clients[0].deliver('{"n":7}'));
    expect(first.result.current.data).toEqual({ n: 7 });
    expect(second.result.current.data).toEqual({ n: 7 });
  });

  it('closes the connection only when the last listener goes, and subscribes again after a reconnect', () => {
    const a = renderHook(() => useStomp<{ n: number }>('/topic/a'));
    const b = renderHook(() => useStomp<{ n: number }>('/topic/b'));
    act(() => clients[0].config.onConnect());

    a.unmount();
    expect(clients[0].deactivate).not.toHaveBeenCalled();
    act(() => clients[0].deliver('{"n":3}', '/topic/b'));
    expect(b.result.current.data).toEqual({ n: 3 });

    // The connection dropped and came back: the remaining topic is subscribed again
    act(() => clients[0].config.onConnect());
    expect(clients[0].subscribe).toHaveBeenCalledTimes(3);

    b.unmount();
    expect(clients[0].deactivate).toHaveBeenCalled();
  });
});
