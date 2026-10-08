import { HelpCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useHelp } from '@/context/HelpContext';

/**
 * Opens the page's help in a sidebar. Shown only when the current page has help to show. The top-bar help
 * button only exists on narrow screens, so the sidebar carries it on wide ones.
 */
export function HelpSidebarButton({ onOpen }: { onOpen?: () => void }) {
  const { helpContent, setIsOpen } = useHelp();
  if (!helpContent) return null;
  return (
    <Button
      variant="ghost"
      size="sm"
      className="w-full justify-start gap-2 text-muted-foreground"
      onClick={() => {
        setIsOpen(true);
        onOpen?.();
      }}
    >
      <HelpCircle className="h-4 w-4" />
      Help
    </Button>
  );
}
