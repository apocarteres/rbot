import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Attempt } from './attempt';
import { time, WEEKDAYS } from './dates';
import { Interval, ScheduleApi, Weekday } from './schedule-api';

interface EditableInterval {
  start: string;
  end: string;
}

interface Day {
  readonly weekday: number;
  readonly title: string;
  readonly intervals: readonly Interval[];
}

// MVP-02
@Component({
  selector: 'app-week-card',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .day { display: flex; flex-wrap: wrap; gap: 8px 12px; align-items: center; padding: 10px 8px; border-bottom: 1px solid var(--line); border-radius: 8px; }
    .day:last-of-type { border-bottom: 0; }
    .day.editing { background: var(--bg); }
    .name { width: 110px; font-weight: 600; }
    .chips { display: flex; flex-wrap: wrap; gap: 6px; flex: 1; min-width: 0; }
    .chip { border: 1px solid var(--line); border-radius: 6px; padding: 2px 8px; font-variant-numeric: tabular-nums; white-space: nowrap; }
    .off { color: var(--muted); flex: 1; }
    .edit { display: flex; flex-direction: column; gap: 8px; flex: 1; min-width: 240px; }
    .interval { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .icon { padding: 6px 10px; }
    @media (max-width: 520px) { .name { width: 100%; } .edit { min-width: 0; } }
  `,
  template: `
    <p class="muted">Часы по местному времени психолога. В день можно задать несколько промежутков.</p>
    @for (day of days(); track day.weekday) {
      <div class="day" [class.editing]="editing() === day.weekday">
        <span class="name">{{ day.title }}</span>
        @if (editing() === day.weekday) {
          <div class="edit">
            @for (interval of draft(); track $index) {
              <div class="interval">
                <input type="time" [attr.aria-label]="day.title + ': начало'" [(ngModel)]="interval.start" [name]="'s' + $index" />
                <span>—</span>
                <input type="time" [attr.aria-label]="day.title + ': конец'" [(ngModel)]="interval.end" [name]="'e' + $index" />
                <button type="button" class="quiet icon" (click)="remove($index)">Убрать</button>
              </div>
            } @empty {
              <span class="off">Выходной</span>
            }
            <div class="actions">
              <button type="button" class="quiet" (click)="add()">Добавить промежуток</button>
              <button type="button" (click)="save(day)" [disabled]="attempt.busy()">Сохранить</button>
              <button type="button" class="quiet" (click)="cancel()">Отмена</button>
            </div>
          </div>
        } @else {
          @if (day.intervals.length) {
            <span class="chips">
              @for (interval of day.intervals; track $index) {
                <span class="chip">{{ label(interval) }}</span>
              }
            </span>
          } @else {
            <span class="off">Выходной</span>
          }
          <button type="button" class="quiet icon" [attr.aria-label]="'Изменить: ' + day.title" (click)="edit(day)">Изменить</button>
        }
      </div>
    }
    @if (attempt.error()) {
      <p class="error" role="alert">{{ attempt.error() }}</p>
    }
    @if (attempt.notice()) {
      <p class="notice" role="status">{{ attempt.notice() }}</p>
    }
  `,
})
export class WeekCard implements OnInit {
  readonly week = input.required<readonly Weekday[]>();
  readonly saved = output<readonly Weekday[]>();

  private readonly api = inject(ScheduleApi);
  protected readonly attempt = new Attempt();
  protected readonly days = signal<readonly Day[]>([]);
  protected readonly editing = signal<number | null>(null);
  protected readonly draft = signal<EditableInterval[]>([]);

  ngOnInit(): void {
    this.days.set(this.week().map((day) => ({ weekday: day.weekday, title: WEEKDAYS[day.weekday - 1], intervals: day.intervals })));
  }

  protected label(interval: Interval): string {
    return `${time(interval.start)}–${time(interval.end)}`;
  }

  protected edit(day: Day): void {
    this.attempt.error.set('');
    this.draft.set(day.intervals.map((one) => ({ start: time(one.start), end: time(one.end) })));
    this.editing.set(day.weekday);
  }

  protected cancel(): void {
    this.editing.set(null);
  }

  protected add(): void {
    const last = this.draft().at(-1);
    this.draft.update((draft) => [...draft, { start: last ? last.end : '10:00', end: last ? '20:00' : '18:00' }]);
  }

  protected remove(index: number): void {
    this.draft.update((draft) => draft.filter((_, at) => at !== index));
  }

  protected save(day: Day): Promise<void> {
    return this.attempt.run(async () => {
      const saved = await this.api.saveWeekday(day.weekday, this.draft().map((one) => ({ start: one.start, end: one.end })));
      this.days.update((days) => days.map((one) => (one.weekday === day.weekday ? { ...one, intervals: saved } : one)));
      this.editing.set(null);
      this.saved.emit(this.days().map((one) => ({ weekday: one.weekday, intervals: one.intervals })));
      return `${day.title}: часы сохранены.`;
    });
  }
}
