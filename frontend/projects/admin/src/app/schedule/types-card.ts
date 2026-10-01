import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { ConfirmDialog } from '../sessions/confirm-dialog';
import { ScheduleApi, SessionFormat, SessionType } from './schedule-api';
import { TypeDialog } from './type-dialog';

const FORMATS: Readonly<Record<SessionFormat, string>> = { IN_PERSON: 'очно', ONLINE: 'онлайн' };
const RUBLES = new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 2 });

type Editing = { readonly type: SessionType | null } | null;

// MVP-02, RBOT-FEAT-016, REQ-CODE-DESIGN-001, REQ-CODE-DESIGN-007
@Component({
  selector: 'app-types-card',
  imports: [TypeDialog, ConfirmDialog],
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
    .icon { display: inline-grid; place-items: center; padding: 8px; line-height: 0; }
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
        <button type="button" class="quiet icon" [attr.aria-label]="'Удалить: ' + type.title" title="Удалить тип" (click)="removing.set(type)">
          <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.8"
            stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M10 11v6M14 11v6M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2l1-12M9 7V4h6v3" /></svg>
        </button>
      </div>
    } @empty {
      <p class="muted">Типов сессий пока нет. Добавьте первый.</p>
    }
    <button type="button" class="quiet add" (click)="editing.set({ type: null })">Добавить тип</button>
    @if (editing(); as open) {
      <app-type-dialog [type]="open.type" (closed)="editing.set(null)" (saved)="applied($event)" />
    }
    @if (removing(); as doomed) {
      <app-confirm-dialog heading="Удалить тип сессии?" [text]="'«' + doomed.title + '» пропадёт из промежутков расписания и из записи. Прошлые записи сохранятся.'"
        confirm="Удалить" [action]="remove(doomed)" (closed)="removing.set(null)" (done)="removed(doomed)" />
    }
  `,
})
export class TypesCard implements OnInit {
  readonly initial = input.required<readonly SessionType[]>();
  readonly saved = output<readonly SessionType[]>();

  protected readonly types = signal<readonly SessionType[]>([]);
  protected readonly editing = signal<Editing>(null);
  protected readonly removing = signal<SessionType | null>(null);
  private readonly api = inject(ScheduleApi);

  ngOnInit(): void {
    this.types.set(this.initial());
  }

  protected meta(type: SessionType): string {
    return [`${type.durationMinutes} мин`, RUBLES.format(type.price), FORMATS[type.format]].join(' · ');
  }

  protected remove(type: SessionType): () => Promise<void> {
    return () => this.api.deleteType(type.id);
  }

  protected removed(type: SessionType): void {
    this.removing.set(null);
    this.types.update((types) => types.filter((one) => one.id !== type.id));
    this.saved.emit(this.types());
  }

  protected applied(type: SessionType): void {
    const known = this.types().some((one) => one.id === type.id);
    this.types.update((types) => (known ? types.map((one) => (one.id === type.id ? type : one)) : [...types, type]));
    this.editing.set(null);
    this.saved.emit(this.types());
  }
}
