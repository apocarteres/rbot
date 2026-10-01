import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';
import { AccountsApi, AccountView } from './accounts-api';

// MVP-01, RBOT-ARC-002, RBOT-ARC-003, RBOT-ARC-004, REQ-AUTH-009, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-new-account-dialog',
  imports: [FormsModule, ApcrAction, ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="new-account-title" apcrModal [apcrModalEscape]="close"
        (submit)="$event.preventDefault()">
        <h2 id="new-account-title">Новая учётная запись</h2>
        <div class="field"><label for="new-email">Почта</label><input id="new-email" name="email" type="email" autocomplete="off" required [(ngModel)]="email" /></div>
        <div class="field"><label for="new-password">Пароль (от 10 символов)</label><input id="new-password" name="password" type="password" autocomplete="new-password" required minlength="10" [(ngModel)]="password" /></div>
        <div class="field"><label for="new-role">Роль</label>
          <select id="new-role" name="role" [(ngModel)]="role">
            <option value="PSYCHOLOGIST">Психолог</option>
            <option value="ADMIN">Администратор</option>
          </select>
        </div>
        @if (failure()) {
          <p class="error" role="alert">{{ failure() }}</p>
        }
        <div class="dialog-actions">
          <button type="button" class="quiet" apcrLocal (click)="close()">Отмена</button>
          <button type="submit" [apcrAction]="save" [apcrActionFailure]="failed" (apcrActionDone)="created.emit($any($event))">Создать</button>
        </div>
      </form>
    </div>
  `,
})
export class NewAccountDialog {
  readonly closed = output<void>();
  readonly created = output<AccountView>();

  private readonly api = inject(AccountsApi);
  protected email = '';
  protected password = '';
  protected role = 'PSYCHOLOGIST';
  protected readonly failure = signal('');
  protected readonly close = (): void => this.closed.emit();
  protected readonly failed = (failure: unknown): void => this.failure.set(failureMessage(failure));
  protected readonly save = (): Promise<AccountView> => {
    this.failure.set('');
    return this.api.create(this.email, this.password, [this.role]);
  };

  constructor() {
    focusFirstField();
  }
}
