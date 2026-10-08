import { useCallback, useRef, useState, type ReactNode } from 'react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

export interface ConfirmOptions {
  title: string;
  description?: ReactNode;
  /** The confirm button's label, such as "Delete". */
  confirmLabel?: string;
  /** Red confirm button, for a step that deletes or can't be undone. */
  destructive?: boolean;
}

/**
 * Asks before a delete or another step that's hard to undo, in place of the browser's own `confirm()`.
 * Render `dialog` once in the component, then `if (!(await confirm({ title: 'Delete track?' }))) return;`.
 */
export function useConfirm() {
  const [options, setOptions] = useState<ConfirmOptions | null>(null);
  const resolveRef = useRef<((confirmed: boolean) => void) | null>(null);

  const confirm = useCallback((next: ConfirmOptions) => {
    // A second ask while one is open answers the first with no
    resolveRef.current?.(false);
    setOptions(next);
    return new Promise<boolean>(resolve => {
      resolveRef.current = resolve;
    });
  }, []);

  function settle(confirmed: boolean) {
    resolveRef.current?.(confirmed);
    resolveRef.current = null;
    setOptions(null);
  }

  const dialog = (
    <Dialog open={options !== null} onOpenChange={open => { if (!open) settle(false); }}>
      <DialogContent showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>{options?.title}</DialogTitle>
          {options?.description && <DialogDescription>{options.description}</DialogDescription>}
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => settle(false)}>
            Cancel
          </Button>
          <Button variant={options?.destructive ? 'destructive' : 'default'} onClick={() => settle(true)}>
            {options?.confirmLabel ?? 'Confirm'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );

  return { confirm, dialog };
}
