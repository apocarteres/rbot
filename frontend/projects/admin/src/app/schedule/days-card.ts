import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AppClock } from '../../../../../shared/clock';
import { Attempt } from './attempt';
import { dayTitle, isoDate, plusDays, time } from './dates';
import { Day, ScheduleApi } from './schedule-api';

const AHEAD_DAYS = 180;

// MVP-02
@Component({
  selector: 'app-days-card',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .forms { display: grid; gap: 16px; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); margin-bottom: 16px; }
    .forms form { border: 1px solid var(--line); border-radius: 8px; padding: 16px; }
    .row { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
    .list { display: flex; flex-direction: column; }
    .item { display: flex; flex-wrap: wrap; gap: 8px 16px; justify-content: space-between; align-items: center; padding: 10px 0; border-top: 1px solid var(--line); }
    .closed { color: var(--danger); }
  `,
  template: `
    <section class="card">
      <h2>Исключения</h2>
      <p class="muted">Отпуск и особые дни заменяют рабочую неделю на выбранные даты.</p>
      <div class="forms">
        <form (ngSubmit)="closeRange()">
          <h3>Отпуск или выходные</h3>
          <div class="row">
            <input type="date" name="from" aria-label="С" required [(ngModel)]="from" />
            <span>—</span>
            <input type="date" name="to" aria-label="По" required [(ngModel)]="to" />
          </div>
          <div class="field"><label for="vacation-note">Заметка</label><input id="vacation-note" name="note" maxlength="200" [(ngModel)]="note" /></div>
          <button type="submit" [disabled]="attempt.busy()">Закрыть дни</button>
        </form>
        <form (ngSubmit)="special()">
          <h3>Особые часы</h3>
          <div class="field"><label for="special-date">Дата</label><input id="special-date" type="date" name="date" required [(ngModel)]="date" /></div>
          <div class="row">
            <input type="time" name="start" aria-label="Начало" required [(ngModel)]="start" />
            <span>—</span>
            <input type="time" name="end" aria-label="Конец" required [(ngModel)]="end" />
          </div>
          <div class="field"><label for="special-note">Заметка</label><input id="special-note" name="note" maxlength="200" [(ngModel)]="specialNote" /></div>
          <button type="submit" [disabled]="attempt.busy()">Задать часы</button>
        </form>
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
  `,
})
export class DaysCard implements OnInit {
  readonly zone = input.required<string>();
  readonly saved = output<void>();

  private readonly api = inject(ScheduleApi);
  private readonly clock = inject(AppClock);
  protected readonly attempt = new Attempt();
  protected readonly days = signal<readonly Day[]>([]);

  protected from = '';
  protected to = '';
  protected note = '';
  protected date = '';
  protected start = '10:00';
  protected end = '14:00';
  protected specialNote = '';

  ngOnInit(): void {
    void this.attempt.run(() => this.load());
  }

  protected title(date: string): string {
    return dayTitle(date);
  }

  protected hours(day: Day): string {
    return day.intervals.map((one) => `${time(one.start)}–${time(one.end)}`).join(', ');
  }

  protected closeRange(): Promise<void> {
    return this.attempt.run(async () => {
      const closed = await this.api.closeDays(this.from, this.to, this.note);
      await this.load();
      this.saved.emit();
      return `Закрыто дней: ${closed.days}.`;
    });
  }

  protected special(): Promise<void> {
    return this.attempt.run(async () => {
      await this.api.saveDay(this.date, false, this.specialNote, [{ start: this.start, end: this.end }]);
      await this.load();
      this.saved.emit();
      return `${dayTitle(this.date)}: особые часы заданы.`;
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
    this.days.set(await this.api.days(today, plusDays(today, AHEAD_DAYS)));
  }
}
