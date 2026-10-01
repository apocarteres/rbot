import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { time } from '../../../../../shared/dates';
import { Interval, SessionType } from './schedule-api';

// MVP-02, RBOT-FEAT-004
@Component({
  selector: 'app-interval-chip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host { display: inline-flex; flex-wrap: wrap; align-items: center; gap: 4px; border: 1px solid var(--line); border-radius: 6px;
      padding: 2px 4px 2px 8px; min-width: 0; }
    .hours { font-variant-numeric: tabular-nums; margin-right: 2px; }
    .tag { font-size: 0.8rem; line-height: 1.4; border-radius: 999px; padding: 1px 8px; white-space: nowrap;
      background: color-mix(in srgb, var(--accent) 12%, transparent); color: var(--accent); }
  `,
  template: `
    <span class="hours">{{ hours() }}</span>
    @for (title of titles(); track title) {
      <span class="tag">{{ title }}</span>
    }
  `,
})
export class IntervalChip {
  readonly interval = input.required<Interval>();
  readonly types = input.required<readonly SessionType[]>();

  protected readonly hours = computed(() => `${time(this.interval().start)}–${time(this.interval().end)}`);
  protected readonly titles = computed(() =>
    this.interval().types.map((id) => this.types().find((one) => one.id === id)?.title ?? 'тип удалён'));
}
