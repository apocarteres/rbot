import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';
import { Cancellation, ClientApi, ClientSession } from '../client-api';
import { when } from '../format';

export type Change = 'cancel' | 'move';

// MVP-05, RBOT-FEAT-002, RBOT-FEAT-026, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-cancel-dialog',
  imports: [ApcrAction, ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .rule { white-space: pre-line; overflow-wrap: anywhere; }
  `,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <section class="dialog" role="dialog" aria-modal="true" aria-labelledby="cancel-title" apcrModal [apcrModalEscape]="close">
        <h2 id="cancel-title">{{ heading() }}</h2>
        <p>{{ session().title }}<br /><span class="muted">{{ period() }}</span></p>
        <p class="rule">{{ decision().text }}</p>
        @if (failure()) {
          <p class="error" role="alert">{{ failure() }}</p>
        }
        <div class="dialog-actions">
          @if (decision().allowed) {
            <button type="button" class="quiet" apcrLocal (click)="close()">Оставить</button>
            <button type="button" [apcrAction]="cancel" [apcrActionFailure]="failed" (apcrActionDone)="cancelled.emit()">Отменить запись</button>
          } @else {
            <button type="button" apcrLocal (click)="close()">Понятно</button>
          }
        </div>
      </section>
    </div>
  `,
})
export class CancelDialog {
  readonly session = input.required<ClientSession>();
  readonly decision = input.required<Cancellation>();
  readonly change = input<Change>('cancel');
  readonly zone = input.required<string>();
  readonly closed = output<void>();
  readonly cancelled = output<void>();

  private readonly api = inject(ClientApi);
  protected readonly failure = signal('');
  protected readonly heading = computed(() => {
    if (this.decision().allowed) {
      return 'Отменить запись?';
    }
    return this.change() === 'move' ? 'Перенести нельзя' : 'Отменить нельзя';
  });
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
