import { signal } from '@angular/core';
import { failureMessage } from '../../../../../shared/failures';

// MVP-02
export class Attempt {
  readonly busy = signal(false);
  readonly error = signal('');
  readonly notice = signal('');

  async run(action: () => Promise<string | void>): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.notice.set('');
    try {
      this.notice.set((await action()) ?? '');
    } catch (failure) {
      this.error.set(failureMessage(failure));
    } finally {
      this.busy.set(false);
    }
  }
}
