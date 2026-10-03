// Keyboard-wedge scanner input.
//
// A keyboard-wedge barcode scanner "types" the scanned code into whatever field has focus,
// followed by an Enter keystroke — indistinguishable from manual typing at the OS level. This
// is a genuinely separate entry point into check-in resolution from BarcodeScanner's camera
// decode path: it never touches the camera or the WASM decoder, and it must keep working even
// when the camera is unavailable or was never granted permission.
import { useState, type FormEvent } from 'react';

export interface KeyboardWedgeInputProps {
  onScan: (code: string) => void;
}

export default function KeyboardWedgeInput({ onScan }: KeyboardWedgeInputProps) {
  const [value, setValue] = useState('');

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const code = value.trim();
    if (!code) return;
    onScan(code);
    setValue('');
  }

  return (
    <form onSubmit={handleSubmit} className="flex items-end gap-2">
      <label className="flex flex-1 flex-col gap-1">
        <span className="text-sm font-medium">Scanner / manual code entry</span>
        <input
          type="text"
          className="rounded border px-2 py-1"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="Scan transponder or type its number…"
        />
      </label>
      <button type="submit" className="rounded bg-slate-700 px-3 py-2 text-white">
        Submit
      </button>
    </form>
  );
}
