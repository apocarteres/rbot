import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Attempt } from './attempt';
import { ScheduleApi, SessionType } from './schedule-api';

type Editable = { -readonly [K in keyof SessionType]: SessionType[K] };

// MVP-02, REQ-CODE-DESIGN-001
@Component({
  selector: 'app-types-card',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .type { display: grid; gap: 8px 12px; grid-template-columns: 2fr 1fr 1fr 1fr; align-items: end; padding: 12px 0; border-bottom: 1px solid var(--line); }
    .flags { display: flex; gap: 16px; flex-wrap: wrap; align-items: center; grid-column: 1 / -1; }
    .flags label { display: flex; gap: 6px; align-items: center; }
    .field { margin: 0; }
    .inactive { color: var(--danger); font-size: 0.85rem; }
    @media (max-width: 640px) { .type { grid-template-columns: 1fr 1fr; } }
  `,
  template: `
    <section class="card">
      <h2>Типы сессий</h2>
      <p class="muted">Клиент записывается только на включённые типы. Цена — в рублях за сессию.</p>
      @for (type of types(); track type.id) {
        <form class="type" (ngSubmit)="save(type)">
          <div class="field"><label [for]="'title-' + type.id">Название</label><input [id]="'title-' + type.id" name="title" maxlength="100" required [(ngModel)]="type.title" /></div>
          <div class="field"><label [for]="'duration-' + type.id">Длительность, мин</label><input [id]="'duration-' + type.id" name="duration" type="number" min="15" max="480" step="5" required [(ngModel)]="type.durationMinutes" /></div>
          <div class="field"><label [for]="'price-' + type.id">Цена, ₽</label><input [id]="'price-' + type.id" name="price" type="number" min="0" step="0.01" required [(ngModel)]="type.price" /></div>
          <div class="field"><label [for]="'format-' + type.id">Формат</label>
            <select [id]="'format-' + type.id" name="format" [(ngModel)]="type.format">
              <option value="IN_PERSON">Очно</option>
              <option value="ONLINE">Онлайн</option>
            </select>
          </div>
          <div class="flags">
            <label><input type="checkbox" name="firstVisit" [(ngModel)]="type.firstVisit" /> Для нового клиента</label>
            <label><input type="checkbox" name="active" [(ngModel)]="type.active" /> Включён</label>
            @if (!type.active) { <span class="inactive">Выключен: клиенты не видят</span> }
            <button type="submit" [disabled]="attempt.busy()">Сохранить</button>
          </div>
        </form>
      }
      <button type="button" class="quiet add" (click)="add()" [disabled]="attempt.busy()">Добавить тип</button>
      @if (attempt.error()) {
        <p class="error" role="alert">{{ attempt.error() }}</p>
      }
      @if (attempt.notice()) {
        <p class="notice" role="status">{{ attempt.notice() }}</p>
      }
    </section>
  `,
})
export class TypesCard implements OnInit {
  readonly initial = input.required<readonly SessionType[]>();
  readonly saved = output<readonly SessionType[]>();

  private readonly api = inject(ScheduleApi);
  protected readonly attempt = new Attempt();
  protected readonly types = signal<Editable[]>([]);

  ngOnInit(): void {
    this.types.set(this.initial().map((type) => ({ ...type })));
  }

  protected save(type: Editable): Promise<void> {
    return this.attempt.run(async () => {
      const saved = await this.api.saveType({ ...type, price: Number(type.price) });
      Object.assign(type, saved);
      this.saved.emit(this.types());
      return `«${saved.title}» сохранён.`;
    });
  }

  protected add(): Promise<void> {
    return this.attempt.run(async () => {
      const created = await this.api.createType({ title: 'Новый тип', durationMinutes: 60, price: 0, format: 'IN_PERSON',
        firstVisit: false, active: false });
      this.types.update((types) => [...types, { ...created }]);
      this.saved.emit(this.types());
      return 'Тип добавлен выключенным: задайте название и цену и включите.';
    });
  }
}
