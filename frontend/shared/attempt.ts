import { signal } from '@angular/core';
import { failureMessage } from './failures';

// MVP-02, RBOT-FEAT-002, RBOT-FEAT-003
export class Attempt {
  readonly busy = signal(false);
  readonly failure = signal('');

  constructor(private readonly texts: Readonly<Record<string, string>> = {}) {}

  async run(action: () => Promise<unknown>): Promise<boolean> {
    if (this.busy()) {
      return false;
    }
    this.busy.set(true);
    this.failure.set('');
    try {
      await action();
      return true;
    } catch (failure) {
      this.failure.set(failureMessage(failure, this.texts));
      return false;
    } finally {
      this.busy.set(false);
    }
  }

  dismiss(): void {
    this.failure.set('');
  }
}
