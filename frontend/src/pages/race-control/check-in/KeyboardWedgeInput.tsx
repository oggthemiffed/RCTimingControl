// Keyboard-wedge scanner input (L11).
//
// A keyboard-wedge barcode scanner "types" the scanned code into whatever field has focus,
// followed by Enter, which looks the same as manual typing. This is a separate entry point
// from BarcodeScanner's camera path: it never touches the camera or the WASM decoder, and it
// must keep working when the camera is unavailable or permission was never granted.
import { useState, type FormEvent } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

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
        <Input
          type="text"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder="Scan transponder or type its number…"
        />
      </label>
      <Button type="submit" variant="secondary">
        Submit
      </Button>
    </form>
  );
}
