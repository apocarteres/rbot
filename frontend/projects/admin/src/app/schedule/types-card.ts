import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Attempt } from './attempt';
import { ScheduleApi, SessionFormat, SessionType } from './schedule-api';

type Editable = { -readonly [K in keyof SessionType]: SessionType[K] };

const FORMATS: Readonly<Record<SessionFormat, string>> = { IN_PERSON: 'очно', ONLINE: 'онлайн' };
const RUBLES = new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 2 });

// MVP-02, REQ-CODE-DESIGN-001, REQ-CODE-DESIGN-007
@Component({
  selector: 'app-types-card',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .type { display: flex; flex-wrap: wrap; gap: 8px 12px; align-items: center; padding: 10px 8px; border-bottom: 1px solid var(--line); border-radius: 8px; }
    .type.editing { background: var(--bg); }
    .about { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
    .title { font-weight: 600; overflow-wrap: anywhere; }
    .meta { color: var(--muted); font-size: 0.9rem; }
    .state { font-size: 0.8rem; border-radius: 6px; padding: 2px 8px; white-space: nowrap; }
    .on { background: color-mix(in srgb, var(--accent) 12%, transparent); color: var(--accent); }
    .off { background: color-mix(in srgb, var(--danger) 10%, transparent); color: var(--danger); }
    form { width: 100%; display: grid; gap: 0 12px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .wide { grid-column: 1 / -1; }
    .flags { grid-column: 1 / -1; display: flex; flex-wrap: wrap; gap: 8px 16px; margin-bottom: 12px; }
    .flags label { display: flex; gap: 6px; align-items: center; }
    .actions { grid-column: 1 / -1; display: flex; flex-wrap: wrap; gap: 8px; }
    .add { margin-top: 12px; }
    @media (max-width: 520px) { form { grid-template-columns: minmax(0, 1fr); } }
  `,
  template: `
    <p class="muted">Клиент записывается только на включённые типы.</p>
    @for (type of types(); track type.id) {
      <div class="type" [class.editing]="editing() === type.id">
        @if (editing() === type.id && draft(); as edit) {
          <form (ngSubmit)="save()">
            <div class="field wide"><label for="type-title">Название</label><input id="type-title" name="title" maxlength="100" required [(ngModel)]="edit.title" /></div>
            <div class="field"><label for="type-duration">Длительность, мин</label><input id="type-duration" name="duration" type="number" min="15" max="480" step="5" required [(ngModel)]="edit.durationMinutes" /></div>
            <div class="field"><label for="type-price">Цена, ₽</label><input id="type-price" name="price" type="number" min="0" step="0.01" required [(ngModel)]="edit.price" /></div>
            <div class="field"><label for="type-format">Формат</label>
              <select id="type-format" name="format" [(ngModel)]="edit.format">
                <option value="IN_PERSON">Очно</option>
                <option value="ONLINE">Онлайн</option>
              </select>
            </div>
            <div class="flags">
              <label><input type="checkbox" name="firstVisit" [(ngModel)]="edit.firstVisit" /> Для нового клиента</label>
              <label><input type="checkbox" name="active" [(ngModel)]="edit.active" /> Включён</label>
            </div>
            <div class="actions">
              <button type="submit" [disabled]="attempt.busy()">Сохранить</button>
              <button type="button" class="quiet" (click)="cancel()">Отмена</button>
            </div>
          </form>
        } @else {
          <span class="about">
            <span class="title">{{ type.title }}</span>
            <span class="meta">{{ meta(type) }}</span>
          </span>
          <span class="state" [class.on]="type.active" [class.off]="!type.active">{{ type.active ? 'Включён' : 'Выключен' }}</span>
          <button type="button" class="quiet" [attr.aria-label]="'Изменить: ' + type.title" (click)="edit(type)">Изменить</button>
        }
      </div>
    }
    <button type="button" class="quiet add" (click)="add()" [disabled]="attempt.busy()">Добавить тип</button>
    @if (attempt.error()) {
      <p class="error" role="alert">{{ attempt.error() }}</p>
    }
    @if (attempt.notice()) {
      <p class="notice" role="status">{{ attempt.notice() }}</p>
    }
  `,
})
export class TypesCard implements OnInit {
  readonly initial = input.required<readonly SessionType[]>();
  readonly saved = output<readonly SessionType[]>();

  private readonly api = inject(ScheduleApi);
  protected readonly attempt = new Attempt();
  protected readonly types = signal<readonly SessionType[]>([]);
  protected readonly editing = signal<string | null>(null);
  protected readonly draft = signal<Editable | null>(null);

  ngOnInit(): void {
    this.types.set(this.initial());
  }

  protected meta(type: SessionType): string {
    return [`${type.durationMinutes} мин`, RUBLES.format(type.price), FORMATS[type.format],
      ...(type.firstVisit ? ['для нового клиента'] : [])].join(' · ');
  }

  protected edit(type: SessionType): void {
    this.attempt.error.set('');
    this.draft.set({ ...type });
    this.editing.set(type.id);
  }

  protected cancel(): void {
    this.editing.set(null);
    this.draft.set(null);
  }

  protected save(): Promise<void> {
    return this.attempt.run(async () => {
      const draft = this.draft();
      if (draft === null) {
        return;
      }
      const saved = await this.api.saveType({ ...draft, price: Number(draft.price) });
      this.types.update((types) => types.map((one) => (one.id === saved.id ? saved : one)));
      this.cancel();
      this.saved.emit(this.types());
      return `«${saved.title}» сохранён.`;
    });
  }

  protected add(): Promise<void> {
    return this.attempt.run(async () => {
      const created = await this.api.createType({ title: 'Новый тип', durationMinutes: 60, price: 0, format: 'IN_PERSON',
        firstVisit: false, active: false });
      this.types.update((types) => [...types, created]);
      this.saved.emit(this.types());
      this.edit(created);
      return 'Тип добавлен выключенным: задайте название и цену и включите.';
    });
  }
}
