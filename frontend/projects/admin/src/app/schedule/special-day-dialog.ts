import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';
import { Day, ScheduleApi } from './schedule-api';

// MVP-02, RBOT-ARC-002, RBOT-ARC-003, RBOT-ARC-004, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-special-day-dialog',
  imports: [FormsModule, ApcrAction, ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="special-day-title" apcrModal [apcrModalEscape]="close"
        (submit)="$event.preventDefault()">
        <h2 id="special-day-title">Особые часы</h2>
        <div class="dialog-grid">
          <div class="field wide"><label for="special-date">Дата</label><input id="special-date" type="date" name="date" required [(ngModel)]="date" /></div>
          <div class="field"><label for="special-start">Начало</label><input id="special-start" type="time" name="start" required [(ngModel)]="start" /></div>
          <div class="field"><label for="special-end">Конец</label><input id="special-end" type="time" name="end" required [(ngModel)]="end" /></div>
          <div class="field wide"><label for="special-note">Заметка</label><input id="special-note" name="note" maxlength="200" [(ngModel)]="note" /></div>
        </div>
        @if (failure()) {
          <p class="error" role="alert">{{ failure() }}</p>
        }
        <div class="dialog-actions">
          <button type="button" class="quiet" apcrLocal (click)="close()">Отмена</button>
          <button type="submit" [apcrAction]="save" [apcrActionFailure]="failed" (apcrActionDone)="saved.emit($any($event))">Задать часы</button>
        </div>
      </form>
    </div>
  `,
})
export class SpecialDayDialog {
  readonly closed = output<void>();
  readonly saved = output<Day>();

  private readonly api = inject(ScheduleApi);
  protected date = '';
  protected start = '10:00';
  protected end = '14:00';
  protected note = '';
  protected readonly failure = signal('');
  protected readonly close = (): void => this.closed.emit();
  protected readonly failed = (failure: unknown): void => this.failure.set(failureMessage(failure));
  protected readonly save = (): Promise<Day> => {
    this.failure.set('');
    return this.api.saveDay(this.date, false, this.note, [{ start: this.start, end: this.end }]);
  };

  constructor() {
    focusFirstField();
  }
}
