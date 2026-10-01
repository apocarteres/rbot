import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Attempt } from './attempt';
import { time, WEEKDAYS } from './dates';
import { Interval, ScheduleApi, Weekday } from './schedule-api';

interface EditableDay {
  readonly weekday: number;
  readonly title: string;
  intervals: { start: string; end: string }[];
}

// MVP-02
@Component({
  selector: 'app-week-card',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .day { display: flex; flex-wrap: wrap; gap: 8px 16px; align-items: flex-start; padding: 12px 0; border-bottom: 1px solid var(--line); }
    .day:last-of-type { border-bottom: 0; }
    .name { width: 120px; font-weight: 600; padding-top: 10px; }
    .hours { display: flex; flex-direction: column; gap: 8px; flex: 1; min-width: 220px; }
    .interval { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .off { color: var(--muted); padding-top: 10px; }
    .actions { display: flex; gap: 8px; flex-wrap: wrap; }
  `,
  template: `
    <section class="card">
      <h2>Рабочая неделя</h2>
      <p class="muted">Часы по местному времени психолога. В день можно задать несколько промежутков.</p>
      @for (day of days(); track day.weekday) {
        <div class="day">
          <span class="name">{{ day.title }}</span>
          <div class="hours">
            @for (interval of day.intervals; track $index) {
              <div class="interval">
                <input type="time" [attr.aria-label]="day.title + ': начало'" [(ngModel)]="interval.start" [name]="'s' + day.weekday + $index" />
                <span>—</span>
                <input type="time" [attr.aria-label]="day.title + ': конец'" [(ngModel)]="interval.end" [name]="'e' + day.weekday + $index" />
                <button type="button" class="quiet" (click)="remove(day, $index)">Убрать</button>
              </div>
            } @empty {
              <span class="off">Выходной</span>
            }
            <div class="actions">
              <button type="button" class="quiet" (click)="add(day)">Добавить промежуток</button>
              <button type="button" (click)="save(day)" [disabled]="attempt.busy()">Сохранить</button>
            </div>
          </div>
        </div>
      }
      @if (attempt.error()) {
        <p class="error" role="alert">{{ attempt.error() }}</p>
      }
      @if (attempt.notice()) {
        <p class="notice" role="status">{{ attempt.notice() }}</p>
      }
    </section>
  `,
})
export class WeekCard implements OnInit {
  readonly week = input.required<readonly Weekday[]>();
  readonly saved = output<void>();

  private readonly api = inject(ScheduleApi);
  protected readonly attempt = new Attempt();
  protected readonly days = signal<EditableDay[]>([]);

  ngOnInit(): void {
    this.days.set(this.week().map((day) => ({
      weekday: day.weekday,
      title: WEEKDAYS[day.weekday - 1],
      intervals: day.intervals.map((one) => ({ start: time(one.start), end: time(one.end) })),
    })));
  }

  protected add(day: EditableDay): void {
    const last = day.intervals.at(-1);
    day.intervals = [...day.intervals, { start: last ? last.end : '10:00', end: last ? '20:00' : '18:00' }];
    this.days.update((days) => [...days]);
  }

  protected remove(day: EditableDay, index: number): void {
    day.intervals = day.intervals.filter((_, at) => at !== index);
    this.days.update((days) => [...days]);
  }

  protected save(day: EditableDay): Promise<void> {
    return this.attempt.run(async () => {
      const intervals: Interval[] = day.intervals.map((one) => ({ start: one.start, end: one.end }));
      const saved = await this.api.saveWeekday(day.weekday, intervals);
      day.intervals = saved.map((one) => ({ start: time(one.start), end: time(one.end) }));
      this.days.update((days) => [...days]);
      this.saved.emit();
      return `${day.title}: часы сохранены.`;
    });
  }
}
