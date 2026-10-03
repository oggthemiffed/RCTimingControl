// Camera-based barcode/QR scanner (KTD6).
//
// Uses the `barcode-detector` package — a BarcodeDetector-shaped ponyfill backed by
// zxing-wasm — instead of the native (Chromium-only) BarcodeDetector Web API, so the same
// code path works in every browser. The WASM binary is bundled as a same-origin build asset
// (via Vite's `?url` import) and the module is configured to load it from there, never from
// a CDN — this app must keep working with zero network connectivity.
import { useEffect, useRef, useState } from 'react';
import { BarcodeDetector, prepareZXingModule } from 'barcode-detector/ponyfill';
import zxingWasmUrl from 'zxing-wasm/reader/zxing_reader.wasm?url';

prepareZXingModule({
  overrides: {
    locateFile: (path: string, prefix: string) =>
      path.endsWith('.wasm') ? zxingWasmUrl : prefix + path,
  },
});

// A couple of common 1D formats plus QR — clubs print either on transponder labels.
const SUPPORTED_FORMATS = ['qr_code', 'code_128', 'ean_13'] as const;

// Don't re-fire the same decoded value while it's still held up in front of the camera.
const DEDUPE_WINDOW_MS = 2000;
const SCAN_INTERVAL_MS = 300;

export interface BarcodeScannerProps {
  onDecode: (code: string) => void;
  onUnavailable: () => void;
}

export default function BarcodeScanner({ onDecode, onUnavailable }: BarcodeScannerProps) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const onDecodeRef = useRef(onDecode);
  const onUnavailableRef = useRef(onUnavailable);
  const [available, setAvailable] = useState(true);

  useEffect(() => {
    onDecodeRef.current = onDecode;
  }, [onDecode]);

  useEffect(() => {
    onUnavailableRef.current = onUnavailable;
  }, [onUnavailable]);

  useEffect(() => {
    let cancelled = false;
    let stream: MediaStream | null = null;
    let intervalId: ReturnType<typeof setInterval> | null = null;

    async function start() {
      if (!navigator.mediaDevices?.getUserMedia) {
        setAvailable(false);
        onUnavailableRef.current();
        return;
      }

      try {
        stream = await navigator.mediaDevices.getUserMedia({ video: true });
      } catch {
        if (!cancelled) {
          setAvailable(false);
          onUnavailableRef.current();
        }
        return;
      }

      if (cancelled) {
        stream.getTracks().forEach((track) => track.stop());
        return;
      }

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }

      const detector = new BarcodeDetector({ formats: [...SUPPORTED_FORMATS] });
      let detecting = false;
      let lastCode: string | null = null;
      let lastAt = 0;

      intervalId = setInterval(() => {
        if (detecting || cancelled || !videoRef.current) return;
        detecting = true;
        detector
          .detect(videoRef.current)
          .then((results) => {
            if (results.length === 0) return;
            const code = results[0].rawValue;
            const now = Date.now();
            if (code !== lastCode || now - lastAt > DEDUPE_WINDOW_MS) {
              lastCode = code;
              lastAt = now;
              onDecodeRef.current(code);
            }
          })
          .catch(() => {
            // Transient per-frame detection errors are expected (e.g. a blurred frame);
            // keep scanning rather than treating them as camera failure.
          })
          .finally(() => {
            detecting = false;
          });
      }, SCAN_INTERVAL_MS);
    }

    start();

    return () => {
      cancelled = true;
      if (intervalId !== null) clearInterval(intervalId);
      stream?.getTracks().forEach((track) => track.stop());
    };
  }, []);

  if (!available) {
    return null;
  }

  return (
    <div className="flex flex-col gap-2">
      <video
        ref={videoRef}
        autoPlay
        muted
        playsInline
        className="w-full max-w-sm rounded border bg-black"
      />
      <p className="text-xs text-slate-500">Point the camera at the transponder label.</p>
    </div>
  );
}
