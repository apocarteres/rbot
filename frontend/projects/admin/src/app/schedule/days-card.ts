import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { AppClock } from '../../../../../shared/clock';
import { Attempt } from '../../../../../shared/attempt';
import { dayTitle, isoDate, plusDays } from '../../../../../shared/dates';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { ClosedDaysDialog } from './closed-days-dialog';
import { IntervalChip } from './interval-chip';
import { Day, ScheduleApi, SessionType } from './schedule-api';
import { SpecialDayDialog } from './special-day-dialog';

const AHEAD_DAYS = 180;

// MVP-02, RBOT-FEAT-003, RBOT-FEAT-004, RBOT-FEAT-006
@Component({
  selector: 'app-days-card',
  imports: [ClosedDaysDialog, SpecialDayDialog, FailureDialog, IntervalChip],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .actions { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
    .list { display: flex; flex-direction: column; }
    .item { display: flex; flex-wrap: wrap; gap: 8px 16px; justify-content: space-between; align-items: center; padding: 10px 0; border-top: 1px solid var(--line); }
    .closed { color: var(--danger); }
    .item > span { flex: 1; min-width: 0; }
    app-interval-chip { margin-left: 8px; }
    .icon { display: inline-grid; place-items: center; padding: 8px; line-height: 0; }
  `,
  template: `
    <section>
      <p class="muted">Отпуск и особые дни заменяют рабочую неделю на выбранные даты.</p>
      <div class="actions">
        <button type="button" (click)="dialog.set('closed')">Отпуск или выходные</button>
        <button type="button" class="quiet" (click)="dialog.set('special')">Особые часы</button>
      </div>
      <div class="list">
        @for (day of days(); track day.date) {
          <div class="item">
            <span><strong>{{ title(day.date) }}</strong>
              @if (day.closed) { · <span class="closed">не работает</span> } @else {
                @for (interval of day.intervals; track $index) { <app-interval-chip [interval]="interval" [types]="types()" /> }
              }
              @if (day.note) { <span class="muted"> · {{ day.note }}</span> }
            </span>
            <button type="button" class="quiet icon" [attr.aria-label]="'Удалить исключение: ' + title(day.date)"
              title="Удалить исключение — вернуть обычные часы" (click)="clear(day)" [disabled]="attempt.busy()">
              <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.8"
                stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M10 11v6M14 11v6M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2l1-12M9 7V4h6v3" /></svg>
            </button>
          </div>
        } @empty {
          <p class="muted">Исключений на ближайшие полгода нет.</p>
        }
      </div>
    </section>
    @switch (dialog()) {
      @case ('closed') { <app-closed-days-dialog (closed)="dialog.set(null)" (saved)="changed()" /> }
      @case ('special') { <app-special-day-dialog [types]="types()" (closed)="dialog.set(null)" (saved)="changed()" /> }
    }
    @if (attempt.failure()) {
      <app-failure-dialog [message]="attempt.failure()" (closed)="attempt.dismiss()" />
    }
  `,
})
export class DaysCard implements OnInit {
  readonly zone = input.required<string>();
  readonly types = input.required<readonly SessionType[]>();
  readonly saved = output<void>();
  readonly counted = output<number>();

  private readonly api = inject(ScheduleApi);
  private readonly clock = inject(AppClock);
  protected readonly attempt = new Attempt();
  protected readonly days = signal<readonly Day[]>([]);

  protected readonly dialog = signal<'closed' | 'special' | null>(null);

  ngOnInit(): void {
    void this.attempt.run(() => this.load());
  }

  protected title(date: string): string {
    return dayTitle(date);
  }

  protected async changed(): Promise<void> {
    this.dialog.set(null);
    await this.attempt.run(async () => {
      await this.load();
      this.saved.emit();
    });
  }

  protected async clear(day: Day): Promise<void> {
    await this.attempt.run(async () => {
      await this.api.clearDay(day.date);
      await this.load();
      this.saved.emit();
    });
  }

  private async load(): Promise<void> {
    const today = isoDate(this.clock.instant(), this.zone());
    const days = await this.api.days(today, plusDays(today, AHEAD_DAYS));
    this.days.set(days);
    this.counted.emit(days.length);
  }
}
