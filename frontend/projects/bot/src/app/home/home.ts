import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthSession } from '@apocarteres/auth';

// MVP-01, MVP-08
@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host { display: grid; place-items: center; min-height: 100dvh; padding: 16px; }
    .card { width: 100%; max-width: 480px; }
    footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 24px; font-size: 0.9rem; }
  `,
  template: `
    <section class="card">
      <h1>Запись к психологу</h1>
      <p>Здесь появятся выбор времени сессии, перенос и отмена записи.</p>
      <p class="muted">Приложение откроется клиентам вместе с ботом в Telegram.</p>
      <footer>
        <span class="muted">{{ session.account()?.email }}</span>
        <button type="button" class="quiet" (click)="logout()">Выйти</button>
      </footer>
    </section>
  `,
})
export class Home {
  protected readonly session = inject(AuthSession);
  private readonly router = inject(Router);

  protected async logout(): Promise<void> {
    await this.session.logout();
    await this.router.navigateByUrl('/login');
  }
}
