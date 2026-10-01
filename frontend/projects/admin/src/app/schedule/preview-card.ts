import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AppClock } from '../../../../../shared/clock';
import { Attempt } from './attempt';
import { clock, dayTitle, isoDate, plusDays } from './dates';
import { ScheduleApi, SessionType, Settings, Slot } from './schedule-api';

const PREVIEW_DAYS = 14;

interface SlotDay {
  readonly date: string;
  readonly times: readonly string[];
}

// MVP-02, REQ-CODE-DESIGN-007
@Component({
  selector: 'app-preview-card',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .preview { padding: 16px; }
    .day { padding: 8px 0; border-top: 1px solid var(--line); font-size: 0.95rem; }
    .small { font-size: 0.8rem; margin: 12px 0 0; }
    .times { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
    .time { border: 1px solid var(--line); border-radius: 6px; padding: 2px 8px; font-variant-numeric: tabular-nums; }
  `,
  template: `
    <section class="card preview">
      <h2>Как увидит клиент</h2>
      @if (!settings().complete) {
        <p class="warning">Заполните параметры записи — без них слотов нет.</p>
      } @else if (types().length === 0) {
        <p class="warning">Включите хотя бы один тип сессии.</p>
      } @else {
        <div class="field">
          <label for="preview-type">Тип сессии</label>
          <select id="preview-type" name="type" [ngModel]="type()" (ngModelChange)="type.set($event)">
            @for (one of types(); track one.id) {
              <option [value]="one.id">{{ one.title }}</option>
            }
          </select>
        </div>
        @if (attempt.error()) {
          <p class="error" role="alert">{{ attempt.error() }}</p>
        }
        @for (day of slotDays(); track day.date) {
          <div class="day">
            <strong>{{ title(day.date) }}</strong>
            <div class="times">
              @for (one of day.times; track one) { <span class="time">{{ one }}</span> }
            </div>
          </div>
        } @empty {
          <p class="muted">Свободного времени нет.</p>
        }
        <p class="muted small">{{ days }} дней · пересчитывается после каждой правки</p>
      }
    </section>
  `,
})
export class PreviewCard {
  readonly settings = input.required<Settings>();
  readonly types = input.required<readonly SessionType[]>();
  readonly version = input(0);

  private readonly api = inject(ScheduleApi);
  private readonly clock = inject(AppClock);
  protected readonly attempt = new Attempt();
  protected readonly days = PREVIEW_DAYS;
  protected readonly type = signal('');
  private readonly slots = signal<readonly Slot[]>([]);

  protected readonly slotDays = computed<readonly SlotDay[]>(() => {
    const zone = this.settings().zone;
    const grouped = new Map<string, string[]>();
    for (const slot of this.slots()) {
      const date = isoDate(Date.parse(slot.start), zone);
      grouped.set(date, [...(grouped.get(date) ?? []), clock(slot.start, zone)]);
    }
    return [...grouped].map(([date, times]) => ({ date, times }));
  });

  constructor() {
    effect(() => {
      const types = this.types();
      if (!types.some((one) => one.id === this.type())) {
        this.type.set(types[0]?.id ?? '');
      }
    });
    effect(() => {
      const type = this.type();
      const settings = this.settings();
      this.version();
      if (type && settings.complete) {
        void this.attempt.run(async () => {
          const today = isoDate(this.clock.instant(), settings.zone);
          this.slots.set(await this.api.slots(type, today, plusDays(today, PREVIEW_DAYS - 1)));
        });
      }
    });
  }

  protected title(date: string): string {
    return dayTitle(date);
  }
}
