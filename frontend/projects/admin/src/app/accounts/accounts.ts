import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NewAccountDialog } from './new-account-dialog';
import { PasswordDialog } from './password-dialog';
import { Attempt } from '../../../../../shared/attempt';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { AccountPage, AccountsApi, AccountView } from './accounts-api';

const ROLE_TITLES: Readonly<Record<string, string>> = { ADMIN: 'администратор', PSYCHOLOGIST: 'психолог', CLIENT: 'клиент' };

// MVP-01, MVP-02, RBOT-FEAT-003, REQ-AUTH-009, REQ-CODE-DESIGN-007
@Component({
  selector: 'app-accounts',
  imports: [FormsModule, NewAccountDialog, PasswordDialog, FailureDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './accounts.scss',
  templateUrl: './accounts.html',
})
export class AccountsPage implements OnInit {
  private readonly api = inject(AccountsApi);

  protected readonly page = signal<AccountPage | null>(null);
  protected readonly attempt = new Attempt();
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
    await this.attempt.run(async () => this.page.set(await this.api.list(this.query, page)));
  }

  protected async created(): Promise<void> {
    this.creating.set(false);
    await this.load(0);
  }

  protected async toggleBlock(account: AccountView): Promise<void> {
    await this.attempt.run(async () => {
      await (account.blocked ? this.api.unblock(account.id) : this.api.block(account.id));
      this.page.set(await this.api.list(this.query, this.page()?.page ?? 0));
    });
  }
}
