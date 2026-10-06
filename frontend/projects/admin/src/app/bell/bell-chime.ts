import { DOCUMENT, inject, Injectable } from '@angular/core';

const NOTES = [{ frequency: 880, delay: 0 }, { frequency: 1318.5, delay: 0.12 }] as const;
const PEAK = 0.12;
const ATTACK = 0.015;
const DECAY = 0.45;
const SILENCE = 0.0001;

// RBOT-FEAT-020
@Injectable({ providedIn: 'root' })
export class BellChime {
  private readonly view = inject(DOCUMENT).defaultView;
  private context: AudioContext | null = null;

  prime(): void {
    const context = this.opened();
    if (context?.state === 'suspended') {
      void context.resume().catch(() => undefined);
    }
  }

  play(): void {
    const context = this.opened();
    if (context?.state === 'running') {
      this.chime(context);
    } else {
      this.prime();
    }
  }

  preview(): void {
    const context = this.opened();
    if (context !== null) {
      void context.resume().then(() => this.chime(context)).catch(() => undefined);
    }
  }

  private opened(): AudioContext | null {
    if (this.context === null && this.view !== null && typeof this.view.AudioContext === 'function'
      && this.view.navigator.userActivation?.hasBeenActive !== false) {
      this.context = new this.view.AudioContext();
    }
    return this.context;
  }

  private chime(context: AudioContext): void {
    const start = context.currentTime;
    for (const note of NOTES) {
      const at = start + note.delay;
      const tone = context.createOscillator();
      const volume = context.createGain();
      tone.type = 'sine';
      tone.frequency.setValueAtTime(note.frequency, at);
      volume.gain.setValueAtTime(0, at);
      volume.gain.linearRampToValueAtTime(PEAK, at + ATTACK);
      volume.gain.exponentialRampToValueAtTime(SILENCE, at + DECAY);
      tone.connect(volume).connect(context.destination);
      tone.start(at);
      tone.stop(at + DECAY);
    }
  }
}
