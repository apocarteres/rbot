import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Attempt } from '../../../../../shared/attempt';
import { AppClock } from '../../../../../shared/clock';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { ScheduleApi } from '../schedule/schedule-api';
import { ClientsApi, ClientView, ConsentView } from './clients-api';
import { ConsentDialog } from './consent-dialog';
import { InviteDialog } from './invite-dialog';

const DATE = new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'short' });

type Inviting = { readonly client: ClientView | null } | null;

// MVP-03, RBOT-FEAT-009, RBOT-FEAT-018, RBOT-FEAT-019, ADR-0002, ADR-0005
@Component({
  selector: 'app-clients',
  imports: [RouterLink, InviteDialog, ConsentDialog, FailureDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .head { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; margin-bottom: 16px; }
    .head h1 { margin: 0; margin-right: auto; }
    .row { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 12px; padding: 10px 0; border-top: 1px solid var(--line); }
    .row:first-child { border-top: 0; }
    .name { flex: 1; min-width: 180px; overflow-wrap: anywhere; }
    .tag { font-size: 0.8rem; border-radius: 999px; padding: 1px 10px; white-space: nowrap; }
    .on { background: color-mix(in srgb, var(--accent) 12%, transparent); color: var(--accent); }
    .wait { background: color-mix(in srgb, var(--danger) 10%, transparent); color: var(--danger); }
    .mail { color: var(--muted); font-size: 0.85rem; }
    .consent { margin-top: 16px; }
    .consent-head { display: flex; flex-wrap: wrap; align-items: baseline; gap: 4px 12px; }
    .consent-head h2 { margin: 0; margin-right: auto; }
    .consent-text { white-space: pre-line; overflow-wrap: anywhere; line-height: 1.45; max-height: 240px; overflow: auto; }
    .small { font-size: 0.85rem; }
  `,
  template: `
    <div class="head">
      <h1>Клиенты</h1>
      <button type="button" [disabled]="nameless()" (click)="inviting.set({ client: null })">Пригласить клиента</button>
    </div>
    @if (nameless()) {
      <p class="warning" role="status">Чтобы приглашать клиентов, заполните <a routerLink="/schedule" [queryParams]="{ tab: 'settings' }">«Имя для клиентов»</a> в правилах записи: его клиент видит в приглашении и в приложении.</p>
    }
    <section class="card">
      @for (one of clients(); track one.id) {
        <div class="row">
          <span class="name">{{ one.name }}</span>
          @switch (one.channel) {
            @case ('TELEGRAM') { <span class="tag on">в Telegram</span> }
            @case ('EMAIL') { <span class="mail">вход по почте</span> }
            @default { <span class="tag wait">{{ pending(one) }}</span> }
          }
          @if (one.channel !== 'EMAIL') {
            <button type="button" class="quiet" [disabled]="nameless()" (click)="inviting.set({ client: one })">Новая ссылка</button>
          }
        </div>
      } @empty {
        @if (loaded()) { <p class="muted">Клиентов пока нет. Пригласите первого — он получит ссылку на бота в Telegram.</p> }
      }
    </section>
    @if (consent(); as text) {
      <section class="card consent" aria-labelledby="consent-title">
        <div class="consent-head">
          <h2 id="consent-title">Согласие на обработку персональных данных</h2>
          <button type="button" class="quiet" (click)="editing.set(true)">Изменить</button>
        </div>
        <p class="muted small">{{ consentNote(text) }}</p>
        <p class="consent-text">{{ text.body }}</p>
      </section>
    }
    @if (editing() && consent(); as text) {
      <app-consent-dialog [consent]="text" (closed)="editing.set(false)" (saved)="consentSaved($event)" />
    }
    @if (inviting(); as open) {
      <app-invite-dialog [client]="open.client" (closed)="inviting.set(null)" (done)="changed()" />
    }
    @if (attempt.failure()) {
      <app-failure-dialog [message]="attempt.failure()" (closed)="attempt.dismiss()" />
    }
  `,
})
export class ClientsPage implements OnInit {
  private readonly api = inject(ClientsApi);
  private readonly schedule = inject(ScheduleApi);
  private readonly clock = inject(AppClock);

  protected readonly attempt = new Attempt();
  protected readonly clients = signal<readonly ClientView[]>([]);
  protected readonly loaded = signal(false);
  protected readonly inviting = signal<Inviting>(null);
  protected readonly consent = signal<ConsentView | null>(null);
  protected readonly editing = signal(false);
  protected readonly nameless = signal(false);

  ngOnInit(): void {
    void this.load();
  }

  protected pending(client: ClientView): string {
    if (!client.inviteExpiresAt || Date.parse(client.inviteExpiresAt) <= this.clock.instant()) {
      return 'ссылка истекла';
    }
    return `приглашён · до ${DATE.format(Date.parse(client.inviteExpiresAt))}`;
  }

  protected consentNote(consent: ConsentView): string {
    if (consent.version === null || consent.savedAt === null) {
      return 'Черновик: клиент прочтёт его в приложении, когда примет приглашение. Замените его текстом, согласованным с юристом.';
    }
    return `Версия ${consent.version} от ${DATE.format(Date.parse(consent.savedAt))}. Клиент читает этот текст в приложении, когда принимает приглашение.`;
  }

  protected consentSaved(consent: ConsentView): void {
    this.consent.set(consent);
    this.editing.set(false);
  }

  protected async changed(): Promise<void> {
    this.inviting.set(null);
    await this.load();
  }

  private async load(): Promise<void> {
    await this.attempt.run(async () => {
      const [clients, consent, settings] = await Promise.all([this.api.list(), this.api.consent(), this.schedule.settings()]);
      this.nameless.set(!settings.displayName?.trim());
      this.clients.set(clients);
      this.consent.set(consent);
      this.loaded.set(true);
    });
  }
}
