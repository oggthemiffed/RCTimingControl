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
  deliver: (body: string) => void;
}

const clients: FakeClient[] = [];

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn().mockImplementation(function (this: unknown, config: FakeClient['config']) {
    let handler: ((msg: { body: string }) => void) | null = null;
    const client: FakeClient = {
      config,
      activate: vi.fn(),
      deactivate: vi.fn(),
      subscribe: vi.fn((_topic: string, h: (msg: { body: string }) => void) => {
        handler = h;
      }),
      deliver: (body) => handler?.({ body }),
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
});
