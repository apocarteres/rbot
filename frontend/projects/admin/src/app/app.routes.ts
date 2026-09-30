import { Routes } from '@angular/router';
import { signedIn, withRole } from '@apocarteres/auth';
import { withUnknownPath } from '@apocarteres/routing';
import { LoginPage } from '../../../../shared/login-page';
import { NotFound } from '../../../../shared/not-found';

// MVP-01, RBOT-OPS-005, REQ-AUTH-015, REQ-DEPLOYMENT-018
export const routes: Routes = withUnknownPath([
  {
    path: 'login',
    component: LoginPage,
    data: { heading: 'Кабинет психолога', hint: 'Вход для психолога и администратора.' },
  },
  {
    path: '',
    canMatch: [signedIn('/login')],
    loadComponent: () => import('./shell/shell').then((m) => m.Shell),
    children: [
      { path: '', loadComponent: () => import('./home/home').then((m) => m.Home) },
      {
        path: 'accounts',
        canMatch: [withRole('ADMIN', '/')],
        loadComponent: () => import('./accounts/accounts').then((m) => m.AccountsPage),
      },
    ],
  },
], { notFound: NotFound });
