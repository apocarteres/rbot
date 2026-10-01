import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';
import { AccountsApi, AccountView } from './accounts-api';

// MVP-01, RBOT-ARC-002, RBOT-ARC-003, RBOT-ARC-004, REQ-AUTH-009, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-password-dialog',
  imports: [FormsModule, ApcrAction, ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="password-title" apcrModal [apcrModalEscape]="close"
        (submit)="$event.preventDefault()">
        <h2 id="password-title">Новый пароль</h2>
        <p class="muted">{{ account().email }}</p>
        <div class="field"><label for="new-password-value">Пароль (от 10 символов)</label><input id="new-password-value" name="password" type="password" autocomplete="new-password" required minlength="10" [(ngModel)]="password" /></div>
        @if (failure()) {
          <p class="error" role="alert">{{ failure() }}</p>
        }
        <div class="dialog-actions">
          <button type="button" class="quiet" apcrLocal (click)="close()">Отмена</button>
          <button type="submit" [apcrAction]="save" [apcrActionFailure]="failed" (apcrActionDone)="changed.emit()">Сохранить</button>
        </div>
      </form>
    </div>
  `,
})
export class PasswordDialog {
  readonly account = input.required<AccountView>();
  readonly closed = output<void>();
  readonly changed = output<void>();

  private readonly api = inject(AccountsApi);
  protected password = '';
  protected readonly failure = signal('');
  protected readonly close = (): void => this.closed.emit();
  protected readonly failed = (failure: unknown): void => this.failure.set(failureMessage(failure));
  protected readonly save = (): Promise<void> => {
    this.failure.set('');
    return this.api.setPassword(this.account().id, this.password);
  };

  constructor() {
    focusFirstField();
  }
}
