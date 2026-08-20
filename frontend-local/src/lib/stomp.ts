// STOMP client wrapper for live race-control data (native WebSocket only — no SockJS, per the
// project's own convention: a club LAN in 2026 does not need a WebSocket fallback). The backend
// exposes `/ws/timing` unauthenticated (see `LocalSecurityConfig`), broadcasting three topics per
// race: `.../timing` (full position-table refresh), `.../state` (lifecycle transitions), and
// `.../marshal` (marshal lap adjustments).
//
// `subscribeToRace` is the low-level, test-friendly surface: connect, subscribe, hand parsed
// payloads to callbacks, return a teardown function. `useRaceChannel` is a thin hook wrapper for
// components. Component tests should `vi.mock('@/lib/stomp', ...)` this whole module rather than
// exercising a real WebSocket — there is no jsdom WebSocket polyfill in this project.
import { Client, type IMessage } from '@stomp/stompjs';
import { useEffect, useState } from 'react';
import type { LiveTimingRowDto } from './api';

export interface RaceStateEvent {
  scheduleId: number;
  newStatus: string;
}

export interface MarshalEvent {
  scheduleId: number;
  entryId: number;
  transponderNumber: string;
  lapDelta: number;
  actingUserName: string;
  adjustedAtEpochMs: number;
}

export interface RaceChannelHandlers {
  onTiming?: (rows: LiveTimingRowDto[]) => void;
  onState?: (event: RaceStateEvent) => void;
  onMarshal?: (event: MarshalEvent) => void;
  onConnectedChange?: (connected: boolean) => void;
}

// Mirrors how api.ts derives its axios baseURL from VITE_API_BASE_URL, but STOMP always needs an
// absolute URL (unlike axios, which is happy with '' + a dev-server proxy) — so an unset env var
// falls back to the current page's origin rather than an empty string.
function resolveBrokerUrl(): string {
  const configured = import.meta.env.VITE_API_BASE_URL;
  const base = configured && configured.length > 0 ? configured : window.location.origin;
  const wsBase = base.replace(/^http/, 'ws').replace(/\/$/, '');
  return `${wsBase}/ws/timing`;
}

export function subscribeToRace(scheduleId: number, handlers: RaceChannelHandlers): () => void {
  const client = new Client({
    brokerURL: resolveBrokerUrl(),
    reconnectDelay: 2000,
  });

  client.onConnect = () => {
    handlers.onConnectedChange?.(true);

    client.subscribe(`/topic/race/${scheduleId}/timing`, (message: IMessage) => {
      handlers.onTiming?.(JSON.parse(message.body) as LiveTimingRowDto[]);
    });
    client.subscribe(`/topic/race/${scheduleId}/state`, (message: IMessage) => {
      handlers.onState?.(JSON.parse(message.body) as RaceStateEvent);
    });
    client.subscribe(`/topic/race/${scheduleId}/marshal`, (message: IMessage) => {
      handlers.onMarshal?.(JSON.parse(message.body) as MarshalEvent);
    });
  };

  // Covers both an unexpected drop (WebSocket close) and a broker-level failure — either way the
  // UI's "reconnecting to local server" indicator needs to flip, and stompjs will keep retrying
  // on its own via reconnectDelay.
  client.onWebSocketClose = () => handlers.onConnectedChange?.(false);
  client.onStompError = () => handlers.onConnectedChange?.(false);

  client.activate();

  return () => {
    handlers.onConnectedChange?.(false);
    void client.deactivate();
  };
}

export interface RaceChannelState {
  rows: LiveTimingRowDto[] | null;
  stateEvent: RaceStateEvent | null;
  marshalEvent: MarshalEvent | null;
  connected: boolean;
}

export function useRaceChannel(scheduleId: number | null): RaceChannelState {
  const [rows, setRows] = useState<LiveTimingRowDto[] | null>(null);
  const [stateEvent, setStateEvent] = useState<RaceStateEvent | null>(null);
  const [marshalEvent, setMarshalEvent] = useState<MarshalEvent | null>(null);
  const [connected, setConnected] = useState(false);

  // Reset local state when `scheduleId` changes. Doing this synchronously during render (React's
  // documented "adjusting state when a prop changes" escape hatch) rather than in an effect body
  // avoids both an extra redundant render and the set-state-in-effect lint rule, which flags
  // unconditional setState calls at the top of an effect.
  const [trackedScheduleId, setTrackedScheduleId] = useState(scheduleId);
  if (scheduleId !== trackedScheduleId) {
    setTrackedScheduleId(scheduleId);
    setRows(null);
    setStateEvent(null);
    setMarshalEvent(null);
    setConnected(false);
  }

  useEffect(() => {
    if (scheduleId === null) {
      return;
    }

    return subscribeToRace(scheduleId, {
      onTiming: setRows,
      onState: setStateEvent,
      onMarshal: setMarshalEvent,
      onConnectedChange: setConnected,
    });
  }, [scheduleId]);

  return { rows, stateEvent, marshalEvent, connected };
}
