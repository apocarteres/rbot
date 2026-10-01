import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { SessionType } from './schedule-api';

// MVP-02, RBOT-FEAT-004, RBOT-FEAT-008, RBOT-FEAT-012
@Component({
  selector: 'app-interval-types',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    fieldset { border: 0; padding: 0; margin: 4px 0 0; display: flex; flex-wrap: wrap; gap: 4px 14px; font-size: 0.9rem; }
    legend { padding: 0; margin-bottom: 4px; color: var(--muted); font-size: 0.85rem; }
    label { display: inline-flex; align-items: center; gap: 6px; }
    .off { color: var(--muted); }
    .note { font-size: 0.8rem; }
  `,
  template: `
    <fieldset>
      <legend>{{ legend() }}</legend>
      @for (type of types(); track type.id) {
        <label [class.off]="!type.active" [title]="type.active ? '' : 'Тип выключен — включите его на вкладке «Типы сессий»'">
          <input type="checkbox" [checked]="selected().includes(type.id)" [disabled]="!type.active && !selected().includes(type.id)"
            (change)="toggle(type.id)" />{{ type.title }}@if (!type.active) { <span class="note">выключен</span> }
        </label>
      }
    </fieldset>
  `,
})
export class IntervalTypes {
  readonly types = input.required<readonly SessionType[]>();
  readonly legend = input('Типы сессий — хотя бы один');
  readonly selected = model<readonly string[]>([]);

  protected toggle(id: string): void {
    this.selected.update((ids) => (ids.includes(id) ? ids.filter((one) => one !== id) : [...ids, id]));
  }
}
