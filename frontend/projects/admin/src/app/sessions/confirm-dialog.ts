import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';

// MVP-05, RBOT-FEAT-005, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-confirm-dialog',
  imports: [ApcrAction, ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-title" apcrModal [apcrModalEscape]="close">
        <h2 id="confirm-title">{{ heading() }}</h2>
        <p>{{ text() }}</p>
        @if (failure()) {
          <p class="error" role="alert">{{ failure() }}</p>
        }
        <div class="dialog-actions">
          <button type="button" class="quiet" apcrLocal (click)="close()">Оставить</button>
          <button type="button" [apcrAction]="run" [apcrActionFailure]="failed" (apcrActionDone)="done.emit()">{{ confirm() }}</button>
        </div>
      </section>
    </div>
  `,
})
export class ConfirmDialog {
  readonly heading = input.required<string>();
  readonly text = input.required<string>();
  readonly confirm = input.required<string>();
  readonly action = input.required<() => Promise<unknown>>();
  readonly closed = output<void>();
  readonly done = output<void>();

  protected readonly failure = signal('');
  protected readonly close = (): void => this.closed.emit();
  protected readonly failed = (failure: unknown): void => this.failure.set(failureMessage(failure));
  protected readonly run = (): Promise<unknown> => {
    this.failure.set('');
    return this.action()();
  };

  constructor() {
    focusFirstField();
  }
}
