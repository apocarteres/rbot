import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthSession } from '@apocarteres/auth';
import { Attempt } from '../../../../../shared/attempt';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { ClientApi, ClientSession } from '../client-api';
import { DEFAULT_ZONE, details, when, zoneNote } from '../format';
import { CancelDialog } from './cancel-dialog';

// MVP-01, MVP-05, MVP-08, RBOT-FEAT-002
@Component({
  selector: 'app-home',
  imports: [RouterLink, CancelDialog, FailureDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host { display: block; max-width: 480px; margin: 0 auto; padding: 24px 16px; }
    .session { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 12px 0; border-top: 1px solid var(--line); }
    .session:first-of-type { border-top: 0; }
    .what { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
    .book { display: block; text-align: center; margin: 16px 0; }
    .small { font-size: 0.85rem; }
    footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 24px; font-size: 0.9rem; }
  `,
  template: `
    <h1>Запись к психологу</h1>
    @if (client()) {
      <section class="card">
        <h2>Ваши записи</h2>
        @for (one of sessions(); track one.id) {
          <div class="session">
            <div class="what">
              <strong>{{ period(one) }}</strong>
              <span class="muted small">{{ one.title }} · {{ details(one) }}</span>
            </div>
            <button type="button" class="quiet" [attr.aria-label]="'Отменить: ' + period(one)" (click)="cancelling.set(one)">Отменить</button>
          </div>
        } @empty {
          @if (loaded()) {
            <p class="muted">Записей пока нет.</p>
          }
        }
        @if (sessions().length > 0) {
          <p class="muted small">{{ note() }}</p>
        }
      </section>
      <a class="button book" routerLink="/book">Записаться</a>
    } @else {
      <section class="card">
        <p>Это приложение для клиентов. Кабинет психолога открывается на admin.yanapaderina.com.</p>
      </section>
    }
    <footer>
      <span class="muted">{{ auth.account()?.email }}</span>
      <button type="button" class="quiet" (click)="logout()">Выйти</button>
    </footer>
    @if (cancelling(); as one) {
      <app-cancel-dialog [session]="one" [zone]="zone()" (closed)="cancelling.set(null)" (cancelled)="cancelled()" />
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
  protected readonly cancelling = signal<ClientSession | null>(null);
  protected readonly client = computed(() => this.auth.account()?.roles.includes('CLIENT') ?? false);
  protected readonly zone = signal(DEFAULT_ZONE);
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

  protected async cancelled(): Promise<void> {
    this.cancelling.set(null);
    await this.load();
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    await this.router.navigateByUrl('/login');
  }

  private async load(): Promise<void> {
    await this.attempt.run(async () => {
      const [offer, sessions] = await Promise.all([this.api.offer(), this.api.sessions()]);
      this.zone.set(offer.zone ?? DEFAULT_ZONE);
      this.sessions.set(sessions);
      this.loaded.set(true);
    });
  }
}
