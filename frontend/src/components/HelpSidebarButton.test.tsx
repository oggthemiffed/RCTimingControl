import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { useEffect } from 'react';
import { HelpProvider, useHelp } from '@/context/HelpContext';
import { HelpSidebarButton } from './HelpSidebarButton';

function Page({ withHelp, onOpen }: { withHelp: boolean; onOpen?: () => void }) {
  const { setHelpContent, isOpen } = useHelp();
  useEffect(() => {
    setHelpContent(withHelp ? <p>About this page</p> : null);
  }, [withHelp, setHelpContent]);
  return (
    <>
      <HelpSidebarButton onOpen={onOpen} />
      <span data-testid="state">{isOpen ? 'open' : 'closed'}</span>
    </>
  );
}

describe('HelpSidebarButton', () => {
  it('opens the help and tells the sidebar to close, when the page has help', () => {
    const onOpen = vi.fn();
    render(
      <HelpProvider>
        <Page withHelp onOpen={onOpen} />
      </HelpProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Help' }));

    expect(screen.getByTestId('state')).toHaveTextContent('open');
    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it('is not shown on a page with no help', () => {
    render(
      <HelpProvider>
        <Page withHelp={false} />
      </HelpProvider>,
    );

    expect(screen.queryByRole('button', { name: 'Help' })).not.toBeInTheDocument();
  });
});
