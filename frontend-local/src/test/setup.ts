import 'fake-indexeddb/auto';
import '@testing-library/jest-dom/vitest';
import { vi } from 'vitest';

// jsdom has no camera/media API at all. Stub navigator.mediaDevices.getUserMedia so
// BarcodeScanner tests can control it per-test (mockResolvedValue / mockRejectedValue)
// instead of throwing "navigator.mediaDevices is undefined".
if (!('mediaDevices' in navigator)) {
  Object.defineProperty(navigator, 'mediaDevices', {
    value: {},
    writable: true,
    configurable: true,
  });
}
if (typeof navigator.mediaDevices.getUserMedia !== 'function') {
  navigator.mediaDevices.getUserMedia = vi.fn();
}
