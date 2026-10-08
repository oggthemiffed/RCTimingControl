import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import SpokenNameEditor from './SpokenNameEditor';
import { adminApi } from '@/lib/adminApi';

vi.mock('@/hooks/admin/useAdminCompetitors', () => ({
  useCompetitorChanges: () => ({ data: undefined }),
  useSetSpokenName: () => ({ mutate: vi.fn(), isPending: false, isError: false }),
}));
vi.mock('@/lib/adminApi', () => ({
  adminApi: { competitors: { previewSpeech: vi.fn() } },
}));

const revoke = vi.fn();
const speak = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(adminApi.competitors.previewSpeech).mockResolvedValue(new Blob(['wav']));
  vi.stubGlobal('URL', { createObjectURL: () => 'blob:clip', revokeObjectURL: revoke });
  vi.stubGlobal('SpeechSynthesisUtterance', class { text: string; constructor(text: string) { this.text = text; } });
  Object.defineProperty(window, 'speechSynthesis', { value: { speak }, configurable: true });
});
afterEach(() => vi.unstubAllGlobals());

function renderEditor() {
  render(
    <SpokenNameEditor
      competitorId={1}
      displayName="Ada Lovelace"
      spokenName={null}
      speechName="Ada Lovelace"
      onSaved={vi.fn()}
      onCancel={vi.fn()}
    />,
  );
}

describe('SpokenNameEditor play', () => {
  it('lets go of the clip when the browser refuses to play it, and falls back to the browser voice', async () => {
    vi.stubGlobal('Audio', class {
      addEventListener() {}
      play() { return Promise.reject(new Error('not allowed')); }
    });
    renderEditor();

    fireEvent.click(screen.getByRole('button', { name: 'Play Ada Lovelace' }));

    await waitFor(() => expect(speak).toHaveBeenCalledTimes(1));
    expect(revoke).toHaveBeenCalledWith('blob:clip');
  });

  it('keeps the clip until it has finished playing', async () => {
    vi.stubGlobal('Audio', class {
      addEventListener() {}
      play() { return Promise.resolve(); }
    });
    renderEditor();

    fireEvent.click(screen.getByRole('button', { name: 'Play Ada Lovelace' }));

    await waitFor(() => expect(adminApi.competitors.previewSpeech).toHaveBeenCalled());
    expect(revoke).not.toHaveBeenCalled();
    expect(speak).not.toHaveBeenCalled();
  });
});
