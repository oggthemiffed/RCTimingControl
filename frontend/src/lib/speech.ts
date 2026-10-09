/** Says the text with the browser's own voice, when it has one (AUDIO-11). */
export function speakWithBrowser(text: string, volume = 1) {
  if (typeof window === 'undefined' || !window.speechSynthesis) return;
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.volume = volume;
  window.speechSynthesis.speak(utterance);
}
