import { Routes } from '@angular/router';
import { signedIn } from '@apocarteres/auth';
import { withUnknownPath } from '@apocarteres/routing';
import { LoginPage } from '../../../../shared/login-page';
import { NotFound } from '../../../../shared/not-found';

// MVP-01, RBOT-OPS-005, REQ-AUTH-015, REQ-DEPLOYMENT-018
export const routes: Routes = withUnknownPath([
  {
    path: 'login',
    component: LoginPage,
    data: { heading: 'Запись к психологу', hint: 'Пока вход только для психолога и администратора.' },
  },
  { path: '', canMatch: [signedIn('/login')], loadComponent: () => import('./home/home').then((m) => m.Home) },
], { notFound: NotFound });
