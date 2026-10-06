import { ChangeDetectionStrategy, Component, computed, effect, inject, input, OnInit, signal } from '@angular/core';
import { Attempt } from '../../../../../shared/attempt';
import { AppClock } from '../../../../../shared/clock';
import { clock, dayTitle, isoDate, mondayOf, plusDays } from '../../../../../shared/dates';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { ScheduleApi, SessionType } from '../schedule/schedule-api';
import { BookDialog } from './book-dialog';
import { ConfirmDialog } from './confirm-dialog';
import { ClientsApi, ClientView } from '../clients/clients-api';
import { CabinetSession, SessionsApi } from './sessions-api';

const RUBLES = new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 2, minimumFractionDigits: 0 });

interface SessionDay {
  readonly date: string;
  readonly sessions: readonly CabinetSession[];
}

interface Confirming {
  readonly heading: string;
  readonly text: string;
  readonly confirm: string;
  readonly action: () => Promise<unknown>;
}

type Editing = { readonly session: CabinetSession | null } | null;

// MVP-05, RBOT-FEAT-005, RBOT-FEAT-009, RBOT-FEAT-020, ADR-0003, REQ-CODE-DESIGN-007
@Component({
  selector: 'app-sessions',
  imports: [BookDialog, ConfirmDialog, FailureDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .head { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; margin-bottom: 16px; }
    .head h1 { margin: 0; margin-right: auto; }
    .week { display: flex; align-items: center; gap: 8px; }
    .range { min-width: 150px; text-align: center; }
    .day { font-size: 1rem; margin: 16px 0 4px; }
    .day:first-child { margin-top: 0; }
    .row { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 12px; padding: 10px 0; border-top: 1px solid var(--line); }
    .time { width: 104px; font-variant-numeric: tabular-nums; font-weight: 600; }
    .who { flex: 1; min-width: 200px; overflow-wrap: anywhere; }
    .closed .time, .closed .who { color: var(--muted); }
    .closed .who strong { text-decoration: line-through; }
    .state { font-size: 0.85rem; color: var(--muted); }
    .actions { display: flex; gap: 8px; margin-left: auto; }
  `,
  template: `
    <div class="head">
      <h1>Записи</h1>
      <div class="week">
        <button type="button" class="quiet" aria-label="Предыдущая неделя" (click)="shift(-7)">←</button>
        <span class="range">{{ range() }}</span>
        <button type="button" class="quiet" aria-label="Следующая неделя" (click)="shift(7)">→</button>
      </div>
      <button type="button" (click)="editing.set({ session: null })">Записать клиента</button>
    </div>
    <section class="card">
      @for (day of days(); track day.date) {
        <h2 class="day">{{ title(day.date) }}</h2>
        @for (one of day.sessions; track one.id) {
          <div class="row" [class.closed]="one.status !== 'BOOKED'">
            <span class="time">{{ period(one) }}</span>
            <span class="who"><strong>{{ one.clientName ?? 'клиент' }}</strong>
              <span class="muted"> · {{ one.title }} · {{ price(one) }}</span></span>
            @if (state(one); as label) { <span class="state">{{ label }}</span> }
            <span class="actions">
              @if (one.status === 'BOOKED' && !started(one)) {
                <button type="button" class="quiet" (click)="editing.set({ session: one })">Перенести</button>
                <button type="button" class="quiet" (click)="cancel(one)">Отменить</button>
              }
              @if (one.status === 'BOOKED' && started(one)) {
                <button type="button" class="quiet" (click)="noShow(one)">Неявка</button>
              }
            </span>
          </div>
        }
      } @empty {
        @if (loaded()) { <p class="muted">На этой неделе записей нет.</p> }
      }
    </section>
    @if (editing(); as open) {
      <app-book-dialog [session]="open.session" [clients]="clients()" [types]="types()" [zone]="zone()" [initialDate]="firstDay()"
        (closed)="editing.set(null)" (saved)="changed()" />
    }
    @if (confirming(); as open) {
      <app-confirm-dialog [heading]="open.heading" [text]="open.text" [confirm]="open.confirm" [action]="open.action"
        (closed)="confirming.set(null)" (done)="changed()" />
    }
    @if (attempt.failure()) {
      <app-failure-dialog [message]="attempt.failure()" (closed)="attempt.dismiss()" />
    }
  `,
})
export class SessionsPage implements OnInit {
  readonly at = input<string | undefined>(undefined);
  private shown: string | undefined;
  private readonly api = inject(SessionsApi);
  private readonly schedule = inject(ScheduleApi);
  private readonly people = inject(ClientsApi);
  private readonly clock = inject(AppClock);

  protected readonly attempt = new Attempt();
  protected readonly zone = signal('Europe/Moscow');
  protected readonly monday = signal('');
  protected readonly sessions = signal<readonly CabinetSession[]>([]);
  protected readonly clients = signal<readonly ClientView[]>([]);
  protected readonly types = signal<readonly SessionType[]>([]);
  protected readonly loaded = signal(false);
  protected readonly editing = signal<Editing>(null);
  protected readonly confirming = signal<Confirming | null>(null);

  protected readonly today = computed(() => isoDate(this.clock.instant(), this.zone()));
  protected readonly firstDay = computed(() => (this.monday() > this.today() ? this.monday() : this.today()));
  protected readonly range = computed(() => {
    const monday = this.monday();
    return monday ? `${short(monday)} — ${short(plusDays(monday, 6))}` : '';
  });
  protected readonly days = computed<readonly SessionDay[]>(() => {
    const grouped = new Map<string, CabinetSession[]>();
    for (const one of this.sessions()) {
      const date = isoDate(Date.parse(one.start), this.zone());
      grouped.set(date, [...(grouped.get(date) ?? []), one]);
    }
    return [...grouped].map(([date, sessions]) => ({ date, sessions }));
  });

  ngOnInit(): void {
    void this.attempt.run(async () => {
      const [settings, types, clients] = await Promise.all([this.schedule.settings(), this.schedule.types(), this.people.list()]);
      this.zone.set(settings.zone);
      this.types.set(types);
      this.clients.set(clients);
      this.shown = this.at();
      this.monday.set(this.weekOf(this.shown));
      await this.load();
    });
  }

  constructor() {
    effect(() => {
      const at = this.at();
      if (this.loaded() && at !== this.shown) {
        this.shown = at;
        this.monday.set(this.weekOf(at));
        void this.attempt.run(() => this.load());
      }
    });
  }

  private weekOf(at: string | undefined): string {
    const instant = at ? Date.parse(at) : Number.NaN;
    return mondayOf(Number.isNaN(instant) ? this.today() : isoDate(instant, this.zone()));
  }

  protected shift(days: number): void {
    this.monday.update((monday) => plusDays(monday, days));
    void this.attempt.run(() => this.load());
  }

  protected title(date: string): string {
    return dayTitle(date);
  }

  protected period(session: CabinetSession): string {
    return `${clock(session.start, this.zone())}–${clock(session.end, this.zone())}`;
  }

  protected price(session: CabinetSession): string {
    return RUBLES.format(session.price);
  }

  protected started(session: CabinetSession): boolean {
    return Date.parse(session.start) <= this.clock.instant();
  }

  protected state(session: CabinetSession): string {
    switch (session.status) {
      case 'CANCELLED':
        return session.rescheduled ? 'перенесена' : session.cancelledByClient ? 'отменил клиент' : 'отменена';
      case 'NO_SHOW':
        return 'неявка';
      case 'COMPLETED':
        return 'прошла';
      default:
        return Date.parse(session.end) <= this.clock.instant() ? 'прошла' : '';
    }
  }

  protected cancel(session: CabinetSession): void {
    this.confirming.set({
      heading: 'Отменить запись?',
      text: `${session.clientName ?? ''} · ${this.title(isoDate(Date.parse(session.start), this.zone()))}, ${this.period(session)}. Штрафа нет.`,
      confirm: 'Отменить запись',
      action: () => this.api.cancel(session.id),
    });
  }

  protected noShow(session: CabinetSession): void {
    this.confirming.set({
      heading: 'Отметить неявку?',
      text: `${session.clientName ?? ''} · ${this.period(session)}.`,
      confirm: 'Отметить неявку',
      action: () => this.api.noShow(session.id),
    });
  }

  protected async changed(): Promise<void> {
    this.editing.set(null);
    this.confirming.set(null);
    await this.attempt.run(() => this.load());
  }

  private async load(): Promise<void> {
    const monday = this.monday();
    this.sessions.set(await this.api.between(monday, plusDays(monday, 6)));
    this.loaded.set(true);
  }
}

function short(date: string): string {
  const [year, month, day] = date.split('-').map(Number);
  return new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'short', timeZone: 'UTC' }).format(Date.UTC(year, month - 1, day));
}
