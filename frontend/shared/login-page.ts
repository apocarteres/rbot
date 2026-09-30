import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthSession } from '@apocarteres/auth';
import { failureMessage } from './failures';

// MVP-01, REQ-AUTH-015
@Component({
  selector: 'app-login-page',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host { display: grid; place-items: center; min-height: 100dvh; padding: 16px; }
    .card { width: 100%; max-width: 380px; }
    button { width: 100%; }
  `,
  template: `
    <form class="card" (ngSubmit)="submit()">
      <h1>{{ heading() }}</h1>
      <p class="muted">{{ hint() }}</p>
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
      <div class="field">
        <label for="email">Почта</label>
        <input id="email" name="email" type="email" autocomplete="username" required [(ngModel)]="email" />
      </div>
      <div class="field">
        <label for="password">Пароль</label>
        <input id="password" name="password" type="password" autocomplete="current-password" required [(ngModel)]="password" />
      </div>
      <button type="submit" [disabled]="busy()">{{ busy() ? 'Вход…' : 'Войти' }}</button>
    </form>
  `,
})
export class LoginPage {
  readonly heading = input('Вход');
  readonly hint = input('');

  private readonly session = inject(AuthSession);
  private readonly router = inject(Router);

  protected email = '';
  protected password = '';
  protected readonly busy = signal(false);
  protected readonly error = signal('');

  protected async submit(): Promise<void> {
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set('');
    try {
      await this.session.login(this.email, this.password);
      this.password = '';
      await this.router.navigateByUrl('/');
    } catch (failure) {
      this.error.set(failureMessage(failure));
    } finally {
      this.busy.set(false);
    }
  }
}
