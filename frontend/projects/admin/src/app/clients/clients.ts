import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Attempt } from '../../../../../shared/attempt';
import { AppClock } from '../../../../../shared/clock';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { ClientsApi, ClientView } from './clients-api';
import { InviteDialog } from './invite-dialog';

const DATE = new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'short' });

type Inviting = { readonly client: ClientView | null } | null;

// MVP-03, RBOT-FEAT-009, ADR-0002
@Component({
  selector: 'app-clients',
  imports: [InviteDialog, FailureDialog],
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
  `,
  template: `
    <div class="head">
      <h1>Клиенты</h1>
      <button type="button" (click)="inviting.set({ client: null })">Пригласить клиента</button>
    </div>
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
            <button type="button" class="quiet" (click)="inviting.set({ client: one })">Новая ссылка</button>
          }
        </div>
      } @empty {
        @if (loaded()) { <p class="muted">Клиентов пока нет. Пригласите первого — он получит ссылку на бота в Telegram.</p> }
      }
    </section>
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
  private readonly clock = inject(AppClock);

  protected readonly attempt = new Attempt();
  protected readonly clients = signal<readonly ClientView[]>([]);
  protected readonly loaded = signal(false);
  protected readonly inviting = signal<Inviting>(null);

  ngOnInit(): void {
    void this.load();
  }

  protected pending(client: ClientView): string {
    if (!client.inviteExpiresAt || Date.parse(client.inviteExpiresAt) <= this.clock.instant()) {
      return 'ссылка истекла';
    }
    return `приглашён · до ${DATE.format(Date.parse(client.inviteExpiresAt))}`;
  }

  protected async changed(): Promise<void> {
    this.inviting.set(null);
    await this.load();
  }

  private async load(): Promise<void> {
    await this.attempt.run(async () => {
      this.clients.set(await this.api.list());
      this.loaded.set(true);
    });
  }
}
