import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';
import { SessionType } from './schedule-api';

// MVP-02, RBOT-FEAT-004, RBOT-FEAT-008
@Component({
  selector: 'app-interval-types',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    fieldset { border: 0; padding: 0; margin: 4px 0 0; display: flex; flex-wrap: wrap; gap: 4px 14px; font-size: 0.9rem; }
    legend { padding: 0; margin-bottom: 4px; color: var(--muted); font-size: 0.85rem; }
    label { display: inline-flex; align-items: center; gap: 6px; }
  `,
  template: `
    <fieldset>
      <legend>{{ legend() }}</legend>
      @for (type of types(); track type.id) {
        <label><input type="checkbox" [checked]="selected().includes(type.id)" (change)="toggle(type.id)" />{{ type.title }}</label>
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
