import { ChangeDetectionStrategy, Component, input, OnInit, output, signal } from '@angular/core';
import { WEEKDAYS } from '../../../../../shared/dates';
import { intervalLabel } from './interval-label';
import { Interval, SessionType, Weekday } from './schedule-api';
import { WeekDayDialog } from './week-day-dialog';

interface Day {
  readonly weekday: number;
  readonly title: string;
  readonly intervals: readonly Interval[];
}

// MVP-02, RBOT-FEAT-004
@Component({
  selector: 'app-week-card',
  imports: [WeekDayDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .day { display: flex; flex-wrap: wrap; gap: 8px 12px; align-items: center; padding: 10px 8px; border-bottom: 1px solid var(--line); }
    .day:last-of-type { border-bottom: 0; }
    .name { width: 110px; font-weight: 600; }
    .day button { margin-left: auto; }
    .chips { display: flex; flex-wrap: wrap; gap: 6px; flex: 1; min-width: 0; }
    .chip { border: 1px solid var(--line); border-radius: 6px; padding: 2px 8px; font-variant-numeric: tabular-nums; min-width: 0; overflow-wrap: anywhere; }
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
    @if (editing(); as day) {
      <app-week-day-dialog [weekday]="day.weekday" [title]="day.title" [intervals]="day.intervals" [types]="types()"
        (closed)="editing.set(null)" (saved)="applied(day, $event)" />
    }
  `,
})
export class WeekCard implements OnInit {
  readonly week = input.required<readonly Weekday[]>();
  readonly types = input.required<readonly SessionType[]>();
  readonly saved = output<readonly Weekday[]>();

  protected readonly days = signal<readonly Day[]>([]);
  protected readonly editing = signal<Day | null>(null);

  ngOnInit(): void {
    this.days.set(this.week().map((day) => ({ weekday: day.weekday, title: WEEKDAYS[day.weekday - 1], intervals: day.intervals })));
  }

  protected label(interval: Interval): string {
    return intervalLabel(interval, this.types());
  }

  protected applied(day: Day, intervals: readonly Interval[]): void {
    this.days.update((days) => days.map((one) => (one.weekday === day.weekday ? { ...one, intervals } : one)));
    this.editing.set(null);
    this.saved.emit(this.days().map((one) => ({ weekday: one.weekday, intervals: one.intervals })));
  }
}
