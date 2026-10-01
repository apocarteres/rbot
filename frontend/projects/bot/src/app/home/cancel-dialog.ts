import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';
import { ClientApi, ClientSession } from '../client-api';
import { when } from '../format';

// MVP-05, RBOT-FEAT-002, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-cancel-dialog',
  imports: [ApcrAction, ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="cancel-title" apcrModal [apcrModalEscape]="close">
        <h2 id="cancel-title">Отменить запись?</h2>
        <p>{{ session().title }}<br /><span class="muted">{{ period() }}</span></p>
        @if (failure()) {
          <p class="error" role="alert">{{ failure() }}</p>
        }
        <div class="dialog-actions">
          <button type="button" class="quiet" apcrLocal (click)="close()">Оставить</button>
          <button type="button" [apcrAction]="cancel" [apcrActionFailure]="failed" (apcrActionDone)="cancelled.emit()">Отменить запись</button>
        </div>
      </section>
    </div>
  `,
})
export class CancelDialog {
  readonly session = input.required<ClientSession>();
  readonly zone = input.required<string>();
  readonly closed = output<void>();
  readonly cancelled = output<void>();

  private readonly api = inject(ClientApi);
  protected readonly failure = signal('');
  protected readonly close = (): void => this.closed.emit();
  protected readonly failed = (failure: unknown): void => this.failure.set(failureMessage(failure));
  protected readonly cancel = (): Promise<ClientSession> => {
    this.failure.set('');
    return this.api.cancel(this.session().id);
  };

  constructor() {
    focusFirstField();
  }

  protected period(): string {
    return when(this.session().start, this.session().end, this.zone());
  }
}
