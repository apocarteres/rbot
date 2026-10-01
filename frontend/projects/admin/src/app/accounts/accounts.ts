import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NewAccountDialog } from './new-account-dialog';
import { PasswordDialog } from './password-dialog';
import { failureMessage } from '../../../../../shared/failures';
import { AccountPage, AccountsApi, AccountView } from './accounts-api';

const ROLE_TITLES: Readonly<Record<string, string>> = { ADMIN: 'администратор', PSYCHOLOGIST: 'психолог' };

// MVP-01, MVP-02, REQ-AUTH-009, REQ-CODE-DESIGN-007
@Component({
  selector: 'app-accounts',
  imports: [FormsModule, NewAccountDialog, PasswordDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './accounts.scss',
  templateUrl: './accounts.html',
})
export class AccountsPage implements OnInit {
  private readonly api = inject(AccountsApi);

  protected readonly page = signal<AccountPage | null>(null);
  protected readonly error = signal('');
  protected readonly notice = signal('');
  protected readonly busy = signal(false);
  protected readonly passwordFor = signal<AccountView | null>(null);
  protected readonly creating = signal(false);

  protected query = '';

  private readonly dates = new Intl.DateTimeFormat('ru-RU', { dateStyle: 'medium', timeStyle: 'short' });

  ngOnInit(): void {
    void this.load(0);
  }

  protected roles(account: AccountView): string {
    return account.roles.map((role) => ROLE_TITLES[role] ?? role).join(', ');
  }

  protected when(value: string | null): string {
    return value ? this.dates.format(new Date(value)) : '—';
  }

  protected pages(page: AccountPage): number {
    return Math.max(1, Math.ceil(page.total / page.size));
  }

  protected async load(page: number): Promise<void> {
    await this.run(async () => this.page.set(await this.api.list(this.query, page)));
  }

  protected async created(account: AccountView): Promise<void> {
    this.creating.set(false);
    await this.run(async () => {
      this.page.set(await this.api.list(this.query, 0));
      this.notice.set(`Учётная запись ${account.email} создана.`);
    });
  }

  protected passwordChanged(account: AccountView): void {
    this.passwordFor.set(null);
    this.notice.set(`Пароль для ${account.email} изменён.`);
  }

  protected async toggleBlock(account: AccountView): Promise<void> {
    await this.run(async () => {
      await (account.blocked ? this.api.unblock(account.id) : this.api.block(account.id));
      this.notice.set(`${account.email}: ${account.blocked ? 'разблокирована' : 'заблокирована'}.`);
      this.page.set(await this.api.list(this.query, this.page()?.page ?? 0));
    });
  }

  private async run(action: () => Promise<void>): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.notice.set('');
    try {
      await action();
    } catch (failure) {
      this.error.set(failureMessage(failure));
    } finally {
      this.busy.set(false);
    }
  }
}
