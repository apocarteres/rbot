import { ChangeDetectionStrategy, Component, computed, effect, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { clock, instantAt, isoDate } from '../../../../../shared/dates';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';
import { ScheduleApi, SessionType, Slot } from '../schedule/schedule-api';
import { CabinetSession, ClientAccount, SessionsApi } from './sessions-api';

const NO_TIME = new Error('время не выбрано');

// MVP-05, RBOT-FEAT-005, ADR-0003, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-book-dialog',
  imports: [FormsModule, ApcrAction, ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .times { display: grid; grid-template-columns: repeat(auto-fill, minmax(72px, 1fr)); gap: 6px; margin-bottom: 12px; }
    .time { background: var(--surface); color: var(--text); border: 1px solid var(--line); padding: 6px 0; font-variant-numeric: tabular-nums; }
    .time[aria-pressed='true'] { border: 2px solid var(--accent); }
    .other { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
    .other input[type='time'] { width: 120px; }
    .small { font-size: 0.85rem; }
  `,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="book-title" apcrModal [apcrModalEscape]="close"
        (submit)="$event.preventDefault()">
        <h2 id="book-title">{{ session() ? 'Перенести запись' : 'Записать клиента' }}</h2>
        @if (session(); as moving) {
          <p class="muted">{{ moving.clientEmail }} · {{ moving.title }}</p>
        } @else {
          <div class="field"><label for="book-client">Клиент</label>
            <select id="book-client" name="client" required [(ngModel)]="client">
              @for (one of clients(); track one.id) { <option [value]="one.id">{{ one.email }}</option> }
            </select>
            @if (clients().length === 0) { <span class="muted small">Клиентов нет: заведите учётную запись с ролью «Клиент».</span> }
          </div>
          <div class="field"><label for="book-type">Тип сессии</label>
            <select id="book-type" name="type" required [ngModel]="type()" (ngModelChange)="type.set($event)">
              @for (one of types(); track one.id) { <option [value]="one.id">{{ one.title }}{{ one.active ? '' : ' (выключен)' }}</option> }
            </select>
          </div>
        }
        <div class="field"><label for="book-date">Дата</label>
          <input id="book-date" name="date" type="date" required [ngModel]="date()" (ngModelChange)="date.set($event)" />
        </div>
        <span class="muted small">Свободное время</span>
        <div class="times">
          @for (one of slots(); track one.start) {
            <button type="button" class="time" apcrLocal [attr.aria-pressed]="!other() && slot()?.start === one.start" (click)="pick(one)">{{ label(one) }}</button>
          } @empty {
            <span class="muted small">Свободного времени в этот день нет.</span>
          }
        </div>
        <label class="other"><input type="checkbox" name="other" [ngModel]="other()" (ngModelChange)="other.set($event)" />Другое время, вне расписания
          @if (other()) { <input type="time" name="time" aria-label="Время начала" [(ngModel)]="time" /> }
        </label>
        @if (other()) {
          <p class="warning small">Время вне свободных слотов: запись создастся, если не пересекается с другой.</p>
        }
        @if (failure()) {
          <p class="error" role="alert">{{ failure() }}</p>
        }
        <div class="dialog-actions">
          <button type="button" class="quiet" apcrLocal (click)="close()">Отмена</button>
          <button type="submit" [apcrAction]="save" [apcrActionFailure]="failed" (apcrActionDone)="saved.emit()">{{ session() ? 'Перенести' : 'Записать' }}</button>
        </div>
      </form>
    </div>
  `,
})
export class BookDialog implements OnInit {
  readonly session = input<CabinetSession | null>(null);
  readonly clients = input<readonly ClientAccount[]>([]);
  readonly types = input<readonly SessionType[]>([]);
  readonly zone = input.required<string>();
  readonly initialDate = input.required<string>();
  readonly closed = output<void>();
  readonly saved = output<void>();

  private readonly schedule = inject(ScheduleApi);
  private readonly sessions = inject(SessionsApi);
  protected client = '';
  protected time = '18:00';
  protected readonly type = signal('');
  protected readonly date = signal('');
  protected readonly other = signal(false);
  protected readonly slot = signal<Slot | null>(null);
  protected readonly slots = signal<readonly Slot[]>([]);
  protected readonly failure = signal('');
  private readonly typeId = computed(() => this.session()?.typeId ?? this.type());

  protected readonly close = (): void => this.closed.emit();
  protected readonly failed = (failure: unknown): void => {
    if (failure !== NO_TIME) {
      this.failure.set(failureMessage(failure));
    }
  };
  protected readonly save = (): Promise<unknown> => {
    this.failure.set('');
    const start = this.start();
    if (!start) {
      this.failure.set('Выберите время.');
      return Promise.reject(NO_TIME);
    }
    const moving = this.session();
    return moving ? this.sessions.reschedule(moving.id, start) : this.sessions.book(this.client, this.type(), start);
  };

  constructor() {
    focusFirstField();
    effect(() => {
      const type = this.typeId();
      const date = this.date();
      this.slot.set(null);
      if (type && date) {
        this.schedule.slots(type, date, date).then((slots) => this.slots.set(slots), () => this.slots.set([]));
      }
    });
  }

  ngOnInit(): void {
    const moving = this.session();
    this.date.set(moving ? isoDate(Date.parse(moving.start), this.zone()) : this.initialDate());
    this.client = this.clients()[0]?.id ?? '';
    this.type.set(this.types().find((one) => one.active)?.id ?? this.types()[0]?.id ?? '');
  }

  protected label(slot: Slot): string {
    return clock(slot.start, this.zone());
  }

  protected pick(slot: Slot): void {
    this.other.set(false);
    this.slot.set(slot);
  }

  private start(): string | null {
    if (this.other()) {
      return this.time ? instantAt(this.date(), this.time, this.zone()) : null;
    }
    return this.slot()?.start ?? null;
  }
}
