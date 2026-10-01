import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';
import { ScheduleApi, SessionType } from './schedule-api';

type Editable = { -readonly [K in keyof SessionType]: SessionType[K] };

// MVP-02, RBOT-FEAT-016, RBOT-ARC-002, RBOT-ARC-003, RBOT-ARC-004, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-type-dialog',
  imports: [FormsModule, ApcrAction, ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .flags { display: flex; flex-wrap: wrap; gap: 8px 16px; margin-bottom: 12px; }
    .flags label { display: flex; gap: 6px; align-items: center; }
  `,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="type-dialog-title" apcrModal [apcrModalEscape]="close"
        (submit)="$event.preventDefault()">
        <h2 id="type-dialog-title">{{ heading() }}</h2>
        <div class="dialog-grid">
          <div class="field wide"><label for="type-title">Название</label><input id="type-title" name="title" maxlength="100" required [(ngModel)]="draft.title" /></div>
          <div class="field"><label for="type-duration">Длительность, мин</label><input id="type-duration" name="duration" type="number" min="15" max="480" step="5" required [(ngModel)]="draft.durationMinutes" /></div>
          <div class="field"><label for="type-buffer">Перерыв после сессии, мин</label><input id="type-buffer" name="buffer" type="number" min="0" max="240" step="5" required [(ngModel)]="draft.bufferMinutes" /></div>
          <div class="field"><label for="type-price">Цена, ₽</label><input id="type-price" name="price" type="number" min="0" step="0.01" required [(ngModel)]="draft.price" /></div>
          <div class="field wide"><label for="type-format">Формат</label>
            <select id="type-format" name="format" [(ngModel)]="draft.format">
              <option value="IN_PERSON">Очно</option>
              <option value="ONLINE">Онлайн</option>
            </select>
          </div>
        </div>
        <div class="flags">
          <label><input type="checkbox" name="active" [(ngModel)]="draft.active" /> Включён</label>
        </div>
        @if (failure()) {
          <p class="error" role="alert">{{ failure() }}</p>
        }
        <div class="dialog-actions">
          <button type="button" class="quiet" apcrLocal (click)="close()">Отмена</button>
          <button type="submit" [apcrAction]="save" [apcrActionFailure]="failed" (apcrActionDone)="saved.emit($any($event))">{{ confirm() }}</button>
        </div>
      </form>
    </div>
  `,
})
export class TypeDialog implements OnInit {
  readonly type = input<SessionType | null>(null);
  readonly closed = output<void>();
  readonly saved = output<SessionType>();

  private readonly api = inject(ScheduleApi);
  protected draft: Editable = { id: '', title: '', durationMinutes: 60, bufferMinutes: 0, price: 0, format: 'IN_PERSON', active: false };
  protected readonly failure = signal('');
  protected readonly heading = computed(() => (this.type() ? 'Тип сессии' : 'Новый тип сессии'));
  protected readonly confirm = computed(() => (this.type() ? 'Сохранить' : 'Добавить'));
  protected readonly close = (): void => this.closed.emit();
  protected readonly failed = (failure: unknown): void => this.failure.set(failureMessage(failure));
  protected readonly save = (): Promise<SessionType> => {
    this.failure.set('');
    const { id, ...fields } = { ...this.draft, price: Number(this.draft.price), bufferMinutes: Number(this.draft.bufferMinutes) };
    return this.type() ? this.api.saveType({ id, ...fields }) : this.api.createType(fields);
  };

  constructor() {
    focusFirstField();
  }

  ngOnInit(): void {
    const type = this.type();
    if (type) {
      this.draft = { ...type };
    }
  }
}
