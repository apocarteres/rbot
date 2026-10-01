import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { AppClock } from '../../../../../shared/clock';
import { Attempt } from './attempt';
import { ClosedDaysDialog } from './closed-days-dialog';
import { dayTitle, isoDate, plusDays, time } from './dates';
import { Day, ScheduleApi } from './schedule-api';
import { SpecialDayDialog } from './special-day-dialog';

const AHEAD_DAYS = 180;

// MVP-02
@Component({
  selector: 'app-days-card',
  imports: [ClosedDaysDialog, SpecialDayDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .actions { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
    .list { display: flex; flex-direction: column; }
    .item { display: flex; flex-wrap: wrap; gap: 8px 16px; justify-content: space-between; align-items: center; padding: 10px 0; border-top: 1px solid var(--line); }
    .closed { color: var(--danger); }
  `,
  template: `
    <section>
      <p class="muted">Отпуск и особые дни заменяют рабочую неделю на выбранные даты.</p>
      <div class="actions">
        <button type="button" (click)="dialog.set('closed')">Отпуск или выходные</button>
        <button type="button" class="quiet" (click)="dialog.set('special')">Особые часы</button>
      </div>
      @if (attempt.error()) {
        <p class="error" role="alert">{{ attempt.error() }}</p>
      }
      @if (attempt.notice()) {
        <p class="notice" role="status">{{ attempt.notice() }}</p>
      }
      <div class="list">
        @for (day of days(); track day.date) {
          <div class="item">
            <span><strong>{{ title(day.date) }}</strong>
              @if (day.closed) { · <span class="closed">не работает</span> } @else { · {{ hours(day) }} }
              @if (day.note) { <span class="muted"> · {{ day.note }}</span> }
            </span>
            <button type="button" class="quiet" (click)="clear(day)" [disabled]="attempt.busy()">Вернуть обычные часы</button>
          </div>
        } @empty {
          <p class="muted">Исключений на ближайшие полгода нет.</p>
        }
      </div>
    </section>
    @switch (dialog()) {
      @case ('closed') { <app-closed-days-dialog (closed)="dialog.set(null)" (saved)="closedDays($event)" /> }
      @case ('special') { <app-special-day-dialog (closed)="dialog.set(null)" (saved)="specialDay($event)" /> }
    }
  `,
})
export class DaysCard implements OnInit {
  readonly zone = input.required<string>();
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

  protected hours(day: Day): string {
    return day.intervals.map((one) => `${time(one.start)}–${time(one.end)}`).join(', ');
  }

  protected closedDays(days: number): Promise<void> {
    this.dialog.set(null);
    return this.attempt.run(async () => {
      await this.load();
      this.saved.emit();
      return `Закрыто дней: ${days}.`;
    });
  }

  protected specialDay(day: Day): Promise<void> {
    this.dialog.set(null);
    return this.attempt.run(async () => {
      await this.load();
      this.saved.emit();
      return `${dayTitle(day.date)}: особые часы заданы.`;
    });
  }

  protected clear(day: Day): Promise<void> {
    return this.attempt.run(async () => {
      await this.api.clearDay(day.date);
      await this.load();
      this.saved.emit();
      return `${dayTitle(day.date)}: обычные часы.`;
    });
  }

  private async load(): Promise<void> {
    const today = isoDate(this.clock.instant(), this.zone());
    const days = await this.api.days(today, plusDays(today, AHEAD_DAYS));
    this.days.set(days);
    this.counted.emit(days.length);
  }
}
