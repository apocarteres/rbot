import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthSession } from '@apocarteres/auth';
import { Attempt } from '../../../../../shared/attempt';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { ApiFailure } from '@apocarteres/http';
import { Cancellation, ClientApi, ClientSession, Consent } from '../client-api';
import { DEFAULT_ZONE, details, when, zoneNote } from '../format';
import { insideTelegram, inviteSettled, pendingInvite } from '../telegram';
import { CancelDialog, Change } from './cancel-dialog';

interface Asked {
  readonly session: ClientSession;
  readonly decision: Cancellation;
  readonly change: Change;
}

// MVP-01, MVP-03, MVP-05, MVP-08, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009, RBOT-FEAT-017, RBOT-FEAT-018, RBOT-FEAT-025, RBOT-FEAT-026, ADR-0005
@Component({
  selector: 'app-home',
  imports: [RouterLink, CancelDialog, FailureDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host { display: block; max-width: 480px; margin: 0 auto; padding: 24px 16px; }
    .session { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 8px 12px; padding: 12px 0; border-top: 1px solid var(--line); }
    .session:first-of-type { border-top: 0; }
    .what { display: flex; flex-direction: column; gap: 2px; min-width: 0; flex: 1 1 220px; }
    .actions { display: flex; align-items: center; gap: 12px; margin-left: auto; }
    .quiet-link { font-size: 0.95rem; }
    .book { display: block; text-align: center; margin: 16px 0; }
    .small { font-size: 0.85rem; }
    .psychologist { margin: -8px 0 16px; color: var(--muted); }
    .psychologist strong { color: var(--text); }
    .consent-text { white-space: pre-line; overflow-wrap: anywhere; line-height: 1.45; }
    .consent-actions { display: flex; flex-direction: column; gap: 8px; margin-top: 16px; }
    footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 24px; font-size: 0.9rem; }
  `,
  template: `
    <h1>Запись к психологу</h1>
    @if (!consent() && psychologists().length > 0) {
      <p class="psychologist">{{ psychologists().length > 1 ? 'Ваши психологи' : 'Ваш психолог' }}: <strong>{{ psychologists().join(', ') }}</strong></p>
    }
    @if (consent(); as one) {
      <section class="card" aria-labelledby="consent-title">
        <h2 id="consent-title">Согласие на обработку персональных данных</h2>
        <p class="muted small">Психолог: {{ one.practiceName }}</p>
        <p class="consent-text">{{ one.text }}</p>
        <div class="consent-actions">
          <button type="button" [disabled]="attempt.busy()" (click)="accept(one)">Согласен</button>
          <span class="muted small">Без согласия запись к этому психологу недоступна. Если не согласны, просто закройте приложение.</span>
        </div>
      </section>
    } @else if (stranger()) {
      <section class="card" aria-labelledby="welcome-title">
        <h2 id="welcome-title">Здравствуйте!</h2>
        <p>Здесь клиенты записываются к психологу, переносят и отменяют сессии.</p>
        <p class="muted">Запись открывается по приглашению. Попросите у психолога ссылку-приглашение и откройте её в Telegram.</p>
      </section>
    } @else if (client() && loaded()) {
      <section class="card">
        <h2>Ваши записи</h2>
        @for (one of sessions(); track one.id) {
          <div class="session">
            <div class="what">
              <strong>{{ period(one) }}</strong>
              <span class="muted small">@if (many()) { {{ one.practiceName }} · }{{ one.title }} · {{ details(one) }}</span>
            </div>
            <span class="actions">
              <button type="button" class="quiet" [disabled]="attempt.busy()" [attr.aria-label]="'Перенести: ' + period(one)" (click)="ask(one, 'move')">Перенести</button>
              <button type="button" class="quiet" [disabled]="attempt.busy()" [attr.aria-label]="'Отменить: ' + period(one)" (click)="ask(one, 'cancel')">Отменить</button>
            </span>
          </div>
        } @empty {
          <p class="muted">Записей пока нет.</p>
        }
        @if (sessions().length > 0) {
          <p class="muted small">{{ note() }}</p>
        }
      </section>
      <a class="button book" routerLink="/book">Записаться</a>
    } @else if (!client()) {
      <section class="card">
        <p>Это приложение для клиентов. Кабинет психолога открывается на admin.yanapaderina.com.</p>
      </section>
    }
    @if (!telegram) {
      <footer>
        <span class="muted">{{ auth.account()?.email }}</span>
        <button type="button" class="quiet" (click)="logout()">Выйти</button>
      </footer>
    }
    @if (cancelling(); as one) {
      <app-cancel-dialog [session]="one.session" [decision]="one.decision" [change]="one.change" [zone]="zone()"
        (closed)="cancelling.set(null)" (cancelled)="cancelled()" />
    }
    @if (attempt.failure()) {
      <app-failure-dialog [message]="attempt.failure()" (closed)="attempt.dismiss()" />
    }
  `,
})
export class Home implements OnInit {
  protected readonly auth = inject(AuthSession);
  private readonly router = inject(Router);
  private readonly api = inject(ClientApi);

  protected readonly attempt = new Attempt();
  protected readonly sessions = signal<readonly ClientSession[]>([]);
  protected readonly loaded = signal(false);
  protected readonly cancelling = signal<Asked | null>(null);
  protected readonly consent = signal<Consent | null>(null);
  protected readonly stranger = signal(false);
  private invited = '';
  protected readonly telegram = insideTelegram();
  protected readonly client = computed(() => this.telegram || (this.auth.account()?.roles.includes('CLIENT') ?? false));
  protected readonly zone = signal(DEFAULT_ZONE);
  protected readonly many = signal(false);
  protected readonly psychologists = signal<readonly string[]>([]);
  protected readonly note = computed(() => zoneNote(this.zone()));

  ngOnInit(): void {
    if (this.client()) {
      void this.load();
    }
  }

  protected period(session: ClientSession): string {
    return when(session.start, session.end, this.zone());
  }

  protected details(session: ClientSession): string {
    return details(session.title, session.format, session.price);
  }

  protected async ask(session: ClientSession, change: Change): Promise<void> {
    const decision = await this.decide(session);
    if (!decision) {
      return;
    }
    if (change === 'move' && decision.allowed) {
      await this.router.navigate(['/book'], { queryParams: { move: session.id } });
      return;
    }
    this.cancelling.set({ session, decision, change });
  }

  protected async cancelled(): Promise<void> {
    this.cancelling.set(null);
    await this.load();
  }

  protected async accept(consent: Consent): Promise<void> {
    const accepted = await this.attempt.run(async () => {
      try {
        if (this.invited) {
          await this.api.acceptInvitation(this.invited, consent.version);
          inviteSettled();
          this.invited = '';
        } else {
          await this.api.consent(consent.practice, consent.version);
        }
      } catch (failure) {
        if (failure instanceof ApiFailure && failure.problem.code === 'consent-outdated') {
          await this.consentDue();
        }
        throw failure;
      }
    });
    if (accepted) {
      this.consent.set(null);
      await this.load();
    }
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }

  private async load(): Promise<void> {
    await this.attempt.run(async () => {
      if (this.telegram && (await this.consentDue())) {
        return;
      }
      const [practices, sessions] = await Promise.all([this.api.practices(), this.api.sessions()]).catch((failure: unknown) => {
        this.stranger.set(failure instanceof ApiFailure && failure.problem.code === 'client-not-linked');
        throw failure;
      });
      this.stranger.set(false);
      this.many.set(practices.length > 1);
      this.psychologists.set(practices.map((one) => one.name));
      if (practices.length > 0) {
        this.zone.set((await this.api.offer(practices[0].id)).zone ?? DEFAULT_ZONE);
      }
      this.sessions.set(sessions);
      this.loaded.set(true);
    });
  }

  private async decide(session: ClientSession): Promise<Cancellation | null> {
    let decision: Cancellation | null = null;
    const done = await this.attempt.run(async () => {
      decision = await this.api.cancellation(session.id);
    });
    return done ? decision : null;
  }

  private async consentDue(): Promise<boolean> {
    const token = pendingInvite();
    if (token) {
      try {
        this.consent.set(await this.api.invitation(token));
        this.invited = token;
        return true;
      } catch (failure) {
        if (!(failure instanceof ApiFailure) || failure.problem.code !== 'invite-rejected') {
          throw failure;
        }
        inviteSettled();
      }
    }
    const due = await this.api.consents();
    this.invited = '';
    this.consent.set(due[0] ?? null);
    return due.length > 0;
  }
}
