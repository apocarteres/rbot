import { ChangeDetectionStrategy, Component, input, OnInit, output, signal } from '@angular/core';
import { time, WEEKDAYS } from './dates';
import { Interval, Weekday } from './schedule-api';
import { WeekDayDialog } from './week-day-dialog';

interface Day {
  readonly weekday: number;
  readonly title: string;
  readonly intervals: readonly Interval[];
}

// MVP-02
@Component({
  selector: 'app-week-card',
  imports: [WeekDayDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .day { display: flex; flex-wrap: wrap; gap: 8px 12px; align-items: center; padding: 10px 8px; border-bottom: 1px solid var(--line); }
    .day:last-of-type { border-bottom: 0; }
    .name { width: 110px; font-weight: 600; }
    .chips { display: flex; flex-wrap: wrap; gap: 6px; flex: 1; min-width: 0; }
    .chip { border: 1px solid var(--line); border-radius: 6px; padding: 2px 8px; font-variant-numeric: tabular-nums; white-space: nowrap; }
    .off { color: var(--muted); flex: 1; }
    @media (max-width: 520px) { .name { width: 100%; } }
  `,
  template: `
    <p class="muted">Часы по местному времени психолога. В день можно задать несколько промежутков.</p>
    @for (day of days(); track day.weekday) {
      <div class="day">
        <span class="name">{{ day.title }}</span>
        @if (day.intervals.length) {
          <span class="chips">
            @for (interval of day.intervals; track $index) {
              <span class="chip">{{ label(interval) }}</span>
            }
          </span>
        } @else {
          <span class="off">Выходной</span>
        }
        <button type="button" class="quiet" [attr.aria-label]="'Изменить: ' + day.title" (click)="editing.set(day)">Изменить</button>
      </div>
    }
    @if (notice()) {
      <p class="notice" role="status">{{ notice() }}</p>
    }
    @if (editing(); as day) {
      <app-week-day-dialog [weekday]="day.weekday" [title]="day.title" [intervals]="day.intervals"
        (closed)="editing.set(null)" (saved)="applied(day, $event)" />
    }
  `,
})
export class WeekCard implements OnInit {
  readonly week = input.required<readonly Weekday[]>();
  readonly saved = output<readonly Weekday[]>();

  protected readonly days = signal<readonly Day[]>([]);
  protected readonly editing = signal<Day | null>(null);
  protected readonly notice = signal('');

  ngOnInit(): void {
    this.days.set(this.week().map((day) => ({ weekday: day.weekday, title: WEEKDAYS[day.weekday - 1], intervals: day.intervals })));
  }

  protected label(interval: Interval): string {
    return `${time(interval.start)}–${time(interval.end)}`;
  }

  protected applied(day: Day, intervals: readonly Interval[]): void {
    this.days.update((days) => days.map((one) => (one.weekday === day.weekday ? { ...one, intervals } : one)));
    this.editing.set(null);
    this.notice.set(`${day.title}: часы сохранены.`);
    this.saved.emit(this.days().map((one) => ({ weekday: one.weekday, intervals: one.intervals })));
  }
}
