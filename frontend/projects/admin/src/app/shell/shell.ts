import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthSession } from '@apocarteres/auth';

// MVP-01
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    header {
      display: flex; flex-wrap: wrap; align-items: center; gap: 12px 24px;
      padding: 12px 16px; background: var(--surface); border-bottom: 1px solid var(--line);
    }
    .brand { font-weight: 600; margin-right: auto; }
    nav { display: flex; gap: 16px; }
    nav a { text-decoration: none; color: var(--muted); }
    nav a.active { color: var(--text); font-weight: 600; }
    .who { display: flex; align-items: center; gap: 12px; font-size: 0.9rem; }
    main { max-width: 960px; margin: 0 auto; padding: 24px 16px; }
  `,
  template: `
    <header>
      <span class="brand">Кабинет психолога</span>
      <nav aria-label="Разделы">
        <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Главная</a>
        @if (session.has('ADMIN')) {
          <a routerLink="/accounts" routerLinkActive="active">Учётные записи</a>
        }
      </nav>
      <span class="who">
        <span class="muted">{{ session.account()?.email }}</span>
        <button type="button" class="quiet" (click)="logout()">Выйти</button>
      </span>
    </header>
    <main><router-outlet /></main>
  `,
})
export class Shell {
  protected readonly session = inject(AuthSession);
  private readonly router = inject(Router);

  protected async logout(): Promise<void> {
    await this.session.logout();
    await this.router.navigateByUrl('/login');
  }
}
