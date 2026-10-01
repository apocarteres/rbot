import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';
import { ScheduleApi } from './schedule-api';

// MVP-02, RBOT-ARC-002, RBOT-ARC-003, RBOT-ARC-004, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-closed-days-dialog',
  imports: [FormsModule, ApcrAction, ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="closed-days-title" apcrModal [apcrModalEscape]="close"
        (submit)="$event.preventDefault()">
        <h2 id="closed-days-title">Отпуск или выходные</h2>
        <div class="dialog-grid">
          <div class="field"><label for="closed-from">С</label><input id="closed-from" type="date" name="from" required [(ngModel)]="from" /></div>
          <div class="field"><label for="closed-to">По</label><input id="closed-to" type="date" name="to" required [(ngModel)]="to" /></div>
          <div class="field wide"><label for="closed-note">Заметка</label><input id="closed-note" name="note" maxlength="200" [(ngModel)]="note" /></div>
        </div>
        @if (failure()) {
          <p class="error" role="alert">{{ failure() }}</p>
        }
        <div class="dialog-actions">
          <button type="button" class="quiet" apcrLocal (click)="close()">Отмена</button>
          <button type="submit" [apcrAction]="save" [apcrActionFailure]="failed" (apcrActionDone)="saved.emit($any($event))">Закрыть дни</button>
        </div>
      </form>
    </div>
  `,
})
export class ClosedDaysDialog {
  readonly closed = output<void>();
  readonly saved = output<number>();

  private readonly api = inject(ScheduleApi);
  protected from = '';
  protected to = '';
  protected note = '';
  protected readonly failure = signal('');
  protected readonly close = (): void => this.closed.emit();
  protected readonly failed = (failure: unknown): void => this.failure.set(failureMessage(failure));
  protected readonly save = async (): Promise<number> => {
    this.failure.set('');
    return (await this.api.closeDays(this.from, this.to || this.from, this.note)).days;
  };

  constructor() {
    focusFirstField();
  }
}
