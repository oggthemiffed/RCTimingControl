import '@testing-library/jest-dom/vitest';

// Mock ResizeObserver (required by Radix UI components in jsdom)
globalThis.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};
