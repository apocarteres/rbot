import { Routes } from '@angular/router';
import { signedIn, withRole } from '@apocarteres/auth';
import { LoginPage } from '../../../../shared/login-page';

// MVP-01, REQ-AUTH-015
export const routes: Routes = [
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
  { path: '**', redirectTo: '' },
];
