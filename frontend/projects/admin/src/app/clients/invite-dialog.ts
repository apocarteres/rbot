import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';
import { ClientsApi, ClientView, InviteView } from './clients-api';

const DATE = new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long' });

// MVP-03, RBOT-FEAT-009, ADR-0002, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-invite-dialog',
  imports: [FormsModule, ApcrAction, ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .link { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
    .link input { flex: 1; font-family: ui-monospace, monospace; font-size: 0.85rem; }
    .small { font-size: 0.85rem; }
  `,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="invite-title" apcrModal [apcrModalEscape]="close"
        (submit)="$event.preventDefault()">
        <h2 id="invite-title">{{ client() ? 'Новая ссылка' : 'Пригласить клиента' }}</h2>
        @if (invite(); as issued) {
          @if (issued.link) {
            <p class="muted small">Ссылка одноразовая, действует до {{ until(issued) }}. Отправьте её клиенту любым удобным способом.</p>
            <div class="link">
              <input id="invite-link" name="link" readonly aria-label="Ссылка-приглашение" [value]="issued.link" />
              <button type="button" class="quiet" apcrLocal (click)="copy(issued.link)">{{ copied() ? 'Скопировано' : 'Копировать' }}</button>
            </div>
          } @else {
            <p class="warning">Бот не настроен: ссылку собрать не из чего. Задайте RBOT_TELEGRAM_BOT_USERNAME на хосте.</p>
          }
          <div class="dialog-actions">
            <button type="button" apcrLocal (click)="done.emit()">Готово</button>
          </div>
        } @else {
          @if (client(); as one) {
            <p>{{ one.name }}</p>
            <p class="muted small">Прежняя ссылка перестанет действовать.</p>
          } @else {
            <div class="field"><label for="invite-label">Как подписать клиента</label>
              <input id="invite-label" name="label" required maxlength="100" placeholder="Анна П." [(ngModel)]="label" />
              <span class="muted small">Подпись видите только вы. Клиент получит ссылку в Telegram на 30 дней.</span>
            </div>
          }
          @if (failure()) {
            <p class="error" role="alert">{{ failure() }}</p>
          }
          <div class="dialog-actions">
            <button type="button" class="quiet" apcrLocal (click)="close()">Отмена</button>
            <button type="submit" [apcrAction]="issue" [apcrActionFailure]="failed" (apcrActionDone)="issued($any($event))">Получить ссылку</button>
          </div>
        }
      </form>
    </div>
  `,
})
export class InviteDialog implements OnInit {
  readonly client = input<ClientView | null>(null);
  readonly closed = output<void>();
  readonly done = output<void>();

  private readonly api = inject(ClientsApi);
  protected label = '';
  protected readonly invite = signal<InviteView | null>(null);
  protected readonly copied = signal(false);
  protected readonly failure = signal('');
  protected readonly close = (): void => (this.invite() ? this.done.emit() : this.closed.emit());
  protected readonly failed = (failure: unknown): void => this.failure.set(failureMessage(failure));
  protected readonly issue = (): Promise<InviteView> => {
    this.failure.set('');
    const one = this.client();
    return one ? this.api.reinvite(one.id) : this.api.invite(this.label);
  };

  constructor() {
    focusFirstField();
  }

  ngOnInit(): void {
    this.copied.set(false);
  }

  protected issued(invite: InviteView): void {
    this.invite.set(invite);
  }

  protected until(invite: InviteView): string {
    return DATE.format(Date.parse(invite.expiresAt));
  }

  protected copy(link: string): void {
    void navigator.clipboard.writeText(link).then(() => this.copied.set(true), () => this.copied.set(false));
  }
}
