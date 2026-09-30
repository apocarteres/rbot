import { Routes } from '@angular/router';
import { signedIn } from '@apocarteres/auth';
import { LoginPage } from '../../../../shared/login-page';

// MVP-01, REQ-AUTH-015
export const routes: Routes = [
  {
    path: 'login',
    component: LoginPage,
    data: { heading: 'Запись к психологу', hint: 'Пока вход только для психолога и администратора.' },
  },
  { path: '', canMatch: [signedIn('/login')], loadComponent: () => import('./home/home').then((m) => m.Home) },
  { path: '**', redirectTo: '' },
];
