import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { Attempt } from '../../../../../shared/attempt';
import { AppClock } from '../../../../../shared/clock';
import { clock, dayTitle, isoDate, mondayOf, plusDays } from '../../../../../shared/dates';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { failureMessage } from '../../../../../shared/failures';
import { Opening, ScheduleApi, SessionType, Settings } from './schedule-api';

const RANGE = new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long', timeZone: 'UTC' });

interface OpeningDay {
  readonly date: string;
  readonly items: readonly Opening[];
}

// RBOT-FEAT-021, RBOT-FEAT-023, MVP-02, ADR-0003, REQ-CODE-DESIGN-007
@Component({
  selector: 'app-openings-card',
  imports: [FailureDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .openings { padding: 16px; }
    .head { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 4px; }
    .head h2 { margin: 0; margin-right: auto; }
    .nav { display: flex; align-items: center; gap: 4px; font-size: 0.9rem; white-space: nowrap; }
    .nav button { padding: 2px 10px; }
    .bulk { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 16px; margin: 8px 0; }
    .bulk .actions { display: flex; gap: 16px; margin-left: auto; }
    .day { padding: 8px 0; border-top: 1px solid var(--line); font-size: 0.95rem; }
    .day-head { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; }
    .times { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
    .time { border: 1px dashed var(--line); border-radius: 6px; padding: 2px 8px; background: none; color: var(--muted);
      font: inherit; font-variant-numeric: tabular-nums; cursor: pointer; }
    .time.open { border: 1px solid var(--accent); background: color-mix(in srgb, var(--accent) 22%, transparent); color: var(--text); font-weight: 600; }
    .time.busy { border-style: solid; background: color-mix(in srgb, var(--muted) 12%, transparent); cursor: default; text-decoration: line-through; }
    .link { background: none; border: 0; padding: 0; color: var(--accent); font-size: 0.85rem; cursor: pointer; }
    .small { font-size: 0.8rem; }
    .legend { display: flex; flex-wrap: wrap; gap: 4px 12px; margin: 12px 0 0; }
    .legend .time { cursor: default; font-size: 0.8rem; }
  `,
  template: `
    <section class="card openings" aria-labelledby="openings-title">
      <div class="head">
        <h2 id="openings-title">Время для записи</h2>
        @if (ready()) {
          <span class="nav">
            <button type="button" class="quiet" aria-label="Предыдущая неделя" [disabled]="!canBack()" (click)="shift(-7)">‹</button>
            <span>{{ range() }}</span>
            <button type="button" class="quiet" aria-label="Следующая неделя" [disabled]="!canForward()" (click)="shift(7)">›</button>
          </span>
        }
      </div>
      @if (!settings().complete) {
        <p class="warning">Заполните правила записи — без них времени нет.</p>
      } @else if (types().length === 0) {
        <p class="warning">Включите хотя бы один тип сессии.</p>
      } @else {
        <p class="muted small">Клиент видит только открытое время. Нажмите на время, чтобы открыть или закрыть его.</p>
        <div class="bulk">
          <span class="muted small count">Открыто {{ opened() }} из {{ total() }}</span>
          <span class="actions">
            <button type="button" class="link" (click)="setAll(true)">Открыть всё</button>
            <button type="button" class="link" (click)="setAll(false)">Закрыть всё</button>
          </span>
        </div>
        @for (day of days(); track day.date) {
          <div class="day">
            <div class="day-head">
              <strong>{{ title(day.date) }}</strong>
              @if (free(day).length > 0) {
                <button type="button" class="link" (click)="setDay(day, !allOpen(day))">{{ allOpen(day) ? 'Закрыть день' : 'Открыть день' }}</button>
              }
            </div>
            @if (day.items.length > 0) {
              <div class="times">
                @for (one of day.items; track one.start) {
                  <button type="button" class="time" [class.open]="one.state === 'OPEN'" [class.busy]="one.state === 'BUSY'"
                    [disabled]="one.state === 'BUSY'" [attr.aria-pressed]="one.state === 'OPEN'" [attr.title]="hint(one)"
                    (click)="toggle(one)">{{ at(one) }}</button>
                }
              </div>
            } @else {
              <span class="muted small">нет времени</span>
            }
          </div>
        }
        <div class="legend muted small">
          <span><span class="time">10:00</span> закрыто</span>
          <span><span class="time open">10:00</span> открыто</span>
          <span><span class="time busy">10:00</span> занято</span>
        </div>
      }
    </section>
    @if (attempt.failure()) {
      <app-failure-dialog [message]="attempt.failure()" (closed)="attempt.dismiss()" />
    }
  `,
})
export class OpeningsCard {
  readonly settings = input.required<Settings>();
  readonly types = input.required<readonly SessionType[]>();
  readonly version = input(0);

  private readonly api = inject(ScheduleApi);
  private readonly clock = inject(AppClock);
  protected readonly attempt = new Attempt();
  private readonly openings = signal<readonly Opening[]>([]);
  private readonly offset = signal(0);
  private queue: Promise<unknown> = Promise.resolve();

  protected readonly ready = computed(() => this.settings().complete && this.types().length > 0);
  private readonly today = computed(() => isoDate(this.clock.instant(), this.settings().zone));
  private readonly last = computed(() => plusDays(this.today(), this.settings().horizonDays ?? 0));
  private readonly monday = computed(() => plusDays(mondayOf(this.today()), this.offset()));
  protected readonly canBack = computed(() => this.offset() > 0);
  protected readonly canForward = computed(() => plusDays(this.monday(), 7) <= this.last());
  protected readonly range = computed(() => `${RANGE.format(Date.parse(this.monday()))} – ${RANGE.format(Date.parse(plusDays(this.monday(), 6)))}`);
  protected readonly opened = computed(() => this.openings().filter((one) => one.state === 'OPEN').length);
  protected readonly total = computed(() => this.openings().filter((one) => one.state !== 'BUSY').length);

  protected readonly days = computed<readonly OpeningDay[]>(() => {
    const zone = this.settings().zone;
    const result: OpeningDay[] = [];
    for (let shift = 0; shift < 7; shift++) {
      const date = plusDays(this.monday(), shift);
      if (date >= this.today() && date <= this.last()) {
        result.push({ date, items: this.openings().filter((one) => isoDate(Date.parse(one.start), zone) === date) });
      }
    }
    return result;
  });

  constructor() {
    effect(() => {
      this.version();
      this.monday();
      if (this.ready()) {
        void this.load();
      }
    });
  }

  protected title(date: string): string {
    return dayTitle(date);
  }

  protected at(opening: Opening): string {
    return clock(opening.start, this.settings().zone);
  }

  protected hint(opening: Opening): string {
    return opening.state === 'BUSY' ? 'Занято записью' : opening.state === 'OPEN' ? 'Открыто — нажмите, чтобы закрыть' : 'Закрыто — нажмите, чтобы открыть';
  }

  protected free(day: OpeningDay): readonly Opening[] {
    return day.items.filter((one) => one.state !== 'BUSY');
  }

  protected allOpen(day: OpeningDay): boolean {
    return this.free(day).every((one) => one.state === 'OPEN');
  }

  protected shift(days: number): void {
    this.offset.update((value) => Math.max(0, value + days));
  }

  protected toggle(opening: Opening): void {
    if (opening.state !== 'BUSY') {
      this.change(opening.state === 'CLOSED' ? [opening.start] : [], opening.state === 'OPEN' ? [opening.start] : []);
    }
  }

  protected setDay(day: OpeningDay, open: boolean): void {
    this.set(this.free(day), open);
  }

  protected setAll(open: boolean): void {
    this.set(this.openings().filter((one) => one.state !== 'BUSY'), open);
  }

  private set(items: readonly Opening[], open: boolean): void {
    const starts = items.filter((one) => (one.state === 'OPEN') !== open).map((one) => one.start);
    if (starts.length > 0) {
      this.change(open ? starts : [], open ? [] : starts);
    }
  }

  private change(open: readonly string[], close: readonly string[]): void {
    const opening = new Set(open);
    const closing = new Set(close);
    this.openings.update((items) => items.map((one) => opening.has(one.start) ? { ...one, state: 'OPEN' as const }
      : closing.has(one.start) ? { ...one, state: 'CLOSED' as const } : one));
    this.queue = this.queue.then(() => this.api.changeOpenings(open, close)).catch(async (failure: unknown) => {
      this.attempt.failure.set(failureMessage(failure));
      await this.load();
    });
  }

  private async load(): Promise<void> {
    const from = this.monday();
    await this.attempt.run(async () => {
      this.openings.set(await this.api.openings(from, plusDays(from, 6)));
    });
  }
}
