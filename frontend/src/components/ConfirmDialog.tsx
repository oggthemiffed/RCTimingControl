import { useCallback, useRef, useState } from 'react';
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
  description?: string;
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
  const [open, setOpen] = useState(false);
  // Kept after closing, so the dialog doesn't go blank while it fades out
  const [options, setOptions] = useState<ConfirmOptions | null>(null);
  const resolveRef = useRef<((confirmed: boolean) => void) | null>(null);

  const confirm = useCallback((next: ConfirmOptions) => {
    // A second ask while one is open answers the first with no
    resolveRef.current?.(false);
    setOptions(next);
    setOpen(true);
    return new Promise<boolean>(resolve => {
      resolveRef.current = resolve;
    });
  }, []);

  function settle(confirmed: boolean) {
    resolveRef.current?.(confirmed);
    resolveRef.current = null;
    setOpen(false);
  }

  const dialog = (
    <Dialog open={open} onOpenChange={next => { if (!next) settle(false); }}>
      <DialogContent showCloseButton={false} {...(options?.description ? {} : { 'aria-describedby': undefined })}>
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
