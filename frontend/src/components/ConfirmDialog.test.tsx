import { describe, it, expect } from 'vitest';
import { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { useConfirm } from './ConfirmDialog';

function Harness() {
  const { confirm, dialog } = useConfirm();
  const [answer, setAnswer] = useState('none');
  return (
    <>
      <button onClick={async () => setAnswer(String(await confirm({ title: 'Delete track?', confirmLabel: 'Delete' })))}>
        Ask
      </button>
      <p>answer: {answer}</p>
      {dialog}
    </>
  );
}

describe('useConfirm', () => {
  it('resolves true when confirmed', async () => {
    render(<Harness />);
    fireEvent.click(screen.getByText('Ask'));
    expect(await screen.findByText('Delete track?')).toBeInTheDocument();
    fireEvent.click(await screen.findByRole('button', { name: 'Delete' }));
    expect(await screen.findByText('answer: true')).toBeInTheDocument();
  });

  it('resolves false when cancelled', async () => {
    render(<Harness />);
    fireEvent.click(screen.getByText('Ask'));
    fireEvent.click(await screen.findByRole('button', { name: 'Cancel' }));
    expect(await screen.findByText('answer: false')).toBeInTheDocument();
  });

  it('resolves false when dismissed with Escape', async () => {
    render(<Harness />);
    fireEvent.click(screen.getByText('Ask'));
    fireEvent.keyDown(await screen.findByRole('dialog'), { key: 'Escape' });
    expect(await screen.findByText('answer: false')).toBeInTheDocument();
  });
});
