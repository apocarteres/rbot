import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';
import { time } from '../../../../../shared/dates';
import { IntervalTypes } from './interval-types';
import { Interval, ScheduleApi, SessionType } from './schedule-api';

interface EditableInterval {
  start: string;
  end: string;
  types: readonly string[];
}

// MVP-02, RBOT-FEAT-004, RBOT-ARC-002, RBOT-ARC-003, RBOT-ARC-004, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-week-day-dialog',
  imports: [FormsModule, ApcrAction, ApcrModal, ApcrModalBackdrop, IntervalTypes],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .interval { padding: 10px 0; border-bottom: 1px solid var(--line); margin-bottom: 8px; }
    .hours { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .off { color: var(--muted); margin: 0 0 12px; }
  `,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="week-day-title" apcrModal [apcrModalEscape]="close"
        (submit)="$event.preventDefault()">
        <h2 id="week-day-title">{{ title() }}</h2>
        @for (interval of draft(); track $index) {
          <div class="interval">
            <div class="hours">
              <input type="time" [attr.aria-label]="'Начало промежутка ' + ($index + 1)" [(ngModel)]="interval.start" [name]="'s' + $index" />
              <span>—</span>
              <input type="time" [attr.aria-label]="'Конец промежутка ' + ($index + 1)" [(ngModel)]="interval.end" [name]="'e' + $index" />
              <button type="button" class="quiet" apcrLocal (click)="remove($index)">Убрать</button>
            </div>
            <app-interval-types [types]="types()" [(selected)]="interval.types" />
          </div>
        } @empty {
          <p class="off">Выходной: промежутков нет.</p>
        }
        <button type="button" class="quiet" apcrLocal (click)="add()">Добавить</button>
        @if (failure()) {
          <p class="error" role="alert">{{ failure() }}</p>
        }
        <div class="dialog-actions">
          <button type="button" class="quiet" apcrLocal (click)="close()">Отмена</button>
          <button type="submit" [apcrAction]="save" [apcrActionFailure]="failed" (apcrActionDone)="saved.emit($any($event))">Сохранить</button>
        </div>
      </form>
    </div>
  `,
})
export class WeekDayDialog implements OnInit {
  readonly weekday = input.required<number>();
  readonly title = input.required<string>();
  readonly intervals = input.required<readonly Interval[]>();
  readonly types = input.required<readonly SessionType[]>();
  readonly closed = output<void>();
  readonly saved = output<readonly Interval[]>();

  private readonly api = inject(ScheduleApi);
  protected readonly draft = signal<EditableInterval[]>([]);
  protected readonly failure = signal('');
  protected readonly close = (): void => this.closed.emit();
  protected readonly failed = (failure: unknown): void => this.failure.set(failureMessage(failure));
  protected readonly save = (): Promise<readonly Interval[]> => {
    this.failure.set('');
    return this.api.saveWeekday(this.weekday(), this.draft().map((one) => ({ start: one.start, end: one.end, types: one.types })));
  };

  constructor() {
    focusFirstField();
  }

  ngOnInit(): void {
    this.draft.set(this.intervals().map((one) => ({ start: time(one.start), end: time(one.end), types: one.types })));
  }

  protected add(): void {
    const last = this.draft().at(-1);
    this.draft.update((draft) => [...draft, { start: last ? last.end : '10:00', end: last ? '20:00' : '18:00', types: [] }]);
  }

  protected remove(index: number): void {
    this.draft.update((draft) => draft.filter((_, at) => at !== index));
  }
}
