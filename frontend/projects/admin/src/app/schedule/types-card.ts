import { ChangeDetectionStrategy, Component, input, OnInit, output, signal } from '@angular/core';
import { SessionFormat, SessionType } from './schedule-api';
import { TypeDialog } from './type-dialog';

const FORMATS: Readonly<Record<SessionFormat, string>> = { IN_PERSON: 'очно', ONLINE: 'онлайн' };
const RUBLES = new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 2 });

type Editing = { readonly type: SessionType | null } | null;

// MVP-02, REQ-CODE-DESIGN-001, REQ-CODE-DESIGN-007
@Component({
  selector: 'app-types-card',
  imports: [TypeDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .type { display: flex; flex-wrap: wrap; gap: 8px 12px; align-items: center; padding: 10px 8px; border-bottom: 1px solid var(--line); }
    .about { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
    .title { font-weight: 600; overflow-wrap: anywhere; }
    .meta { color: var(--muted); font-size: 0.9rem; }
    .state { font-size: 0.8rem; border-radius: 6px; padding: 2px 8px; white-space: nowrap; }
    .on { background: color-mix(in srgb, var(--accent) 12%, transparent); color: var(--accent); }
    .off { background: color-mix(in srgb, var(--danger) 10%, transparent); color: var(--danger); }
    .add { margin-top: 12px; }
  `,
  template: `
    <p class="muted">Клиент записывается только на включённые типы.</p>
    @for (type of types(); track type.id) {
      <div class="type">
        <span class="about">
          <span class="title">{{ type.title }}</span>
          <span class="meta">{{ meta(type) }}</span>
        </span>
        <span class="state" [class.on]="type.active" [class.off]="!type.active">{{ type.active ? 'Включён' : 'Выключен' }}</span>
        <button type="button" class="quiet" [attr.aria-label]="'Изменить: ' + type.title" (click)="editing.set({ type })">Изменить</button>
      </div>
    }
    <button type="button" class="quiet add" (click)="editing.set({ type: null })">Добавить тип</button>
    @if (notice()) {
      <p class="notice" role="status">{{ notice() }}</p>
    }
    @if (editing(); as open) {
      <app-type-dialog [type]="open.type" (closed)="editing.set(null)" (saved)="applied($event)" />
    }
  `,
})
export class TypesCard implements OnInit {
  readonly initial = input.required<readonly SessionType[]>();
  readonly saved = output<readonly SessionType[]>();

  protected readonly types = signal<readonly SessionType[]>([]);
  protected readonly editing = signal<Editing>(null);
  protected readonly notice = signal('');

  ngOnInit(): void {
    this.types.set(this.initial());
  }

  protected meta(type: SessionType): string {
    return [`${type.durationMinutes} мин`, RUBLES.format(type.price), FORMATS[type.format],
      ...(type.firstVisit ? ['для нового клиента'] : [])].join(' · ');
  }

  protected applied(type: SessionType): void {
    const known = this.types().some((one) => one.id === type.id);
    this.types.update((types) => (known ? types.map((one) => (one.id === type.id ? type : one)) : [...types, type]));
    this.editing.set(null);
    this.notice.set(`«${type.title}» сохранён.`);
    this.saved.emit(this.types());
  }
}
