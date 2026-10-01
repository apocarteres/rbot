import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Attempt } from '../../../../../shared/attempt';
import { AppClock } from '../../../../../shared/clock';
import { clock, dayTitle, isoDate, plusDays } from '../../../../../shared/dates';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { ClientApi, ClientSession, Offer, OfferedType, Practice, Slot } from '../client-api';
import { DEFAULT_ZONE, details, shortDay, when, zoneNote } from '../format';

type Step = 'practice' | 'type' | 'day' | 'time' | 'confirm' | 'done';

const STEPS: readonly Step[] = ['type', 'day', 'time', 'confirm'];
const MOVE_STEPS: readonly Step[] = ['day', 'time', 'confirm'];
const CLOSED: Offer = { open: false, zone: null, leadMinutes: null, horizonDays: null, types: [] };

interface SlotDay {
  readonly date: string;
  readonly slots: readonly Slot[];
}

// MVP-05, MVP-08, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-017, ADR-0003, REQ-CODE-DESIGN-007
@Component({
  selector: 'app-booking-wizard',
  imports: [RouterLink, FailureDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host { display: block; max-width: 480px; margin: 0 auto; padding: 24px 16px; }
    .top { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
    .back { padding: 6px 10px; }
    .progress { height: 4px; background: var(--line); border-radius: 2px; margin-bottom: 20px; }
    .progress span { display: block; height: 4px; background: var(--accent); border-radius: 2px; }
    .options { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
    .option { text-align: left; background: var(--surface); color: var(--text); border: 1px solid var(--line); display: flex; flex-direction: column; gap: 2px; }
    .chips { display: grid; grid-template-columns: repeat(auto-fill, minmax(96px, 1fr)); gap: 8px; margin-bottom: 16px; }
    .chip { background: var(--surface); color: var(--text); border: 1px solid var(--line); padding: 10px 4px; font-variant-numeric: tabular-nums; }
    .chip[aria-pressed='true'], .option[aria-pressed='true'] { border: 2px solid var(--accent); }
    .small { font-size: 0.85rem; }
    .wide { width: 100%; }
    .done { text-align: center; }
  `,
  template: `
    @if (step() !== 'done') {
      <div class="top">
        @if (step() === steps()[0]) {
          <a class="quiet" routerLink="/">На главную</a>
        } @else {
          <button type="button" class="quiet back" aria-label="Назад" (click)="back()">←</button>
        }
        <span class="muted small">Шаг {{ number() }} из {{ steps().length }}</span>
      </div>
      <div class="progress" aria-hidden="true"><span [style.width.%]="number() * 100 / steps().length"></span></div>
    }
    @if (step() === 'practice') {
      <h1>К кому записаться?</h1>
      <div class="options">
        @for (one of practices(); track one.id) {
          <button type="button" class="option" [attr.aria-pressed]="practice() === one.id" (click)="choosePractice(one)">
            <strong>{{ one.name }}</strong>
          </button>
        }
      </div>
    } @else if (offer(); as current) {
      @if (!current.open || current.types.length === 0) {
        <section class="card">
          <h1>Запись пока закрыта</h1>
          <p class="muted">Психолог ещё не открыл запись. Загляните позже.</p>
          <a routerLink="/">На главную</a>
        </section>
      } @else {
        @switch (step()) {
          @case ('type') {
            <h1>Какая встреча нужна?</h1>
            <div class="options">
              @for (one of current.types; track one.id) {
                <button type="button" class="option" [attr.aria-pressed]="type()?.id === one.id" (click)="chooseType(one)">
                  <strong>{{ one.title }}</strong>
                  <span class="muted small">{{ one.durationMinutes }} мин · {{ details(one) }}</span>
                </button>
              }
            </div>
          }
          @case ('day') {
            <h1>{{ moving() ? 'Перенести на день' : 'Выберите день' }}</h1>
            @if (moving(); as old) { <p class="muted small">Сейчас: {{ old.title }}, {{ when(old) }}</p> }
            <div class="chips">
              @for (one of days(); track one.date) {
                <button type="button" class="chip" [attr.aria-pressed]="day() === one.date" (click)="chooseDay(one.date)">{{ short(one.date) }}</button>
              } @empty {
                <p class="muted">Свободного времени нет. Попробуйте другой тип сессии или загляните позже.</p>
              }
            </div>
          }
          @case ('time') {
            <h1>{{ title(day() ?? '') }}</h1>
            <div class="chips">
              @for (one of times(); track one.start) {
                <button type="button" class="chip" [attr.aria-pressed]="slot()?.start === one.start" (click)="chooseSlot(one)">{{ time(one) }}</button>
              }
            </div>
            <p class="muted small">{{ note() }}</p>
          }
          @case ('confirm') {
            <h1>{{ moving() ? 'Проверьте перенос' : 'Проверьте запись' }}</h1>
            @if (type(); as chosen) {
              <section class="card">
                <p><strong>{{ chosen.title }}</strong></p>
                <p>{{ period() }}</p>
                <p class="muted small">{{ details(chosen) }}, оплата психологу</p>
                <p class="muted small">{{ note() }}. {{ cancelNote() }}</p>
                @if (moving(); as old) { <p class="muted small">Вместо: {{ when(old) }}</p> }
                <button type="button" class="wide" [disabled]="attempt.busy()" (click)="book()">{{ moving() ? 'Перенести' : 'Записаться' }}</button>
              </section>
            }
          }
          @case ('done') {
            <section class="card done">
              <h1>{{ moving() ? 'Запись перенесена' : 'Вы записаны' }}</h1>
              <p>{{ booked()?.title }}<br /><span class="muted">{{ period() }}</span></p>
              <a class="button" routerLink="/">На главную</a>
            </section>
          }
        }
      }
    }
    @if (attempt.failure()) {
      <app-failure-dialog [message]="attempt.failure()" (closed)="attempt.dismiss()" />
    }
  `,
})
export class BookingWizard implements OnInit {
  private readonly api = inject(ClientApi);
  private readonly clock = inject(AppClock);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly attempt = new Attempt();
  protected readonly step = signal<Step>('type');
  protected readonly practices = signal<readonly Practice[]>([]);
  protected readonly practice = signal('');
  protected readonly offer = signal<Offer | null>(null);
  protected readonly type = signal<OfferedType | null>(null);
  protected readonly slots = signal<readonly Slot[]>([]);
  protected readonly day = signal<string | null>(null);
  protected readonly slot = signal<Slot | null>(null);
  protected readonly booked = signal<ClientSession | null>(null);
  protected readonly moving = signal<ClientSession | null>(null);
  protected readonly steps = computed<readonly Step[]>(() => {
    if (this.moving()) {
      return MOVE_STEPS;
    }
    return this.practices().length > 1 ? ['practice', ...STEPS] : STEPS;
  });

  protected readonly zone = computed(() => this.offer()?.zone ?? DEFAULT_ZONE);
  protected readonly number = computed(() => this.steps().indexOf(this.step()) + 1);
  protected readonly note = computed(() => zoneNote(this.zone()));
  protected readonly days = computed<readonly SlotDay[]>(() => {
    const grouped = new Map<string, Slot[]>();
    for (const one of this.slots()) {
      const date = isoDate(Date.parse(one.start), this.zone());
      grouped.set(date, [...(grouped.get(date) ?? []), one]);
    }
    return [...grouped].map(([date, slots]) => ({ date, slots }));
  });
  protected readonly times = computed(() => this.days().find((one) => one.date === this.day())?.slots ?? []);
  protected readonly cancelNote = computed(() => {
    const lead = this.offer()?.leadMinutes ?? 0;
    return lead > 0 ? `Перенести или отменить можно не позже чем за ${hours(lead)} до начала` : 'Перенести или отменить можно до начала';
  });

  ngOnInit(): void {
    const move = this.route.snapshot.queryParamMap.get('move');
    void this.attempt.run(async () => {
      const [practices, sessions] = await Promise.all([this.api.practices(), move ? this.api.sessions() : Promise.resolve([])]);
      this.practices.set(practices);
      const old = sessions.find((one) => one.id === move);
      if (old) {
        this.practice.set(old.practice);
        const offer = await this.api.offer(old.practice);
        this.offer.set(offer);
        const type = offer.types.find((one) => one.id === old.typeId);
        if (type) {
          this.moving.set(old);
          this.type.set(type);
          await this.loadSlots();
          this.step.set('day');
        }
        return;
      }
      if (practices.length === 1) {
        this.practice.set(practices[0].id);
        this.offer.set(await this.api.offer(practices[0].id));
      } else if (practices.length > 1) {
        this.step.set('practice');
      } else {
        this.offer.set(CLOSED);
      }
    });
  }

  protected details(type: OfferedType): string {
    return details(type.title, type.format, type.price);
  }

  protected short(date: string): string {
    return shortDay(date);
  }

  protected title(date: string): string {
    return date ? dayTitle(date) : '';
  }

  protected time(slot: Slot): string {
    return clock(slot.start, this.zone());
  }

  protected period(): string {
    const chosen = this.slot();
    return chosen ? when(chosen.start, chosen.end, this.zone()) : '';
  }

  protected async choosePractice(practice: Practice): Promise<void> {
    this.practice.set(practice.id);
    this.type.set(null);
    if (await this.attempt.run(async () => this.offer.set(await this.api.offer(practice.id)))) {
      this.step.set('type');
    }
  }

  protected async chooseType(type: OfferedType): Promise<void> {
    this.type.set(type);
    this.day.set(null);
    this.slot.set(null);
    if (await this.attempt.run(() => this.loadSlots())) {
      this.step.set('day');
    }
  }

  protected chooseDay(date: string): void {
    this.day.set(date);
    this.slot.set(null);
    this.step.set('time');
  }

  protected chooseSlot(slot: Slot): void {
    this.slot.set(slot);
    this.step.set('confirm');
  }

  protected back(): void {
    const steps = this.steps();
    const at = steps.indexOf(this.step());
    if (at > 0) {
      this.step.set(steps[at - 1]);
    } else {
      void this.router.navigateByUrl('/');
    }
  }

  protected async book(): Promise<void> {
    const type = this.type();
    const slot = this.slot();
    if (!type || !slot) {
      return;
    }
    const old = this.moving();
    const done = await this.attempt.run(async () =>
      this.booked.set(await (old ? this.api.reschedule(old.id, slot.start) : this.api.book(this.practice(), type.id, slot.start))));
    if (done) {
      this.step.set('done');
      return;
    }
    await this.loadSlots().catch(() => undefined);
    if (!this.slots().some((one) => one.start === slot.start)) {
      this.slot.set(null);
      this.step.set(this.times().length > 0 ? 'time' : 'day');
    }
  }

  protected when(session: ClientSession): string {
    return when(session.start, session.end, this.zone());
  }

  private async loadSlots(): Promise<void> {
    const type = this.type();
    const offer = this.offer();
    if (!type || !offer) {
      return;
    }
    const today = isoDate(this.clock.instant(), this.zone());
    this.slots.set(await this.api.slots(this.practice(), type.id, today, plusDays(today, offer.horizonDays ?? 14)));
  }
}

function hours(minutes: number): string {
  return minutes % 60 === 0 ? `${minutes / 60} ч` : `${minutes} мин`;
}
