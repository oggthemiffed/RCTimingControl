import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';

type Props = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  eventId: number;
};

export function RoundGeneratorWizard({ open, onOpenChange, eventId: _eventId }: Props) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Generate Rounds</DialogTitle>
          <DialogDescription>
            Round generation is configured in the Admin panel under Event &rarr; Classes.
            Once class formats are set, rounds are generated automatically.
          </DialogDescription>
        </DialogHeader>
      </DialogContent>
    </Dialog>
  );
}
