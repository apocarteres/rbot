import { Routes } from '@angular/router';
import { withUnknownPath } from '@apocarteres/routing';
import { LoginPage } from '../../../../shared/login-page';
import { NotFound } from '../../../../shared/not-found';
import { clientEntry } from './telegram';

// MVP-01, MVP-08, RBOT-FEAT-002, RBOT-FEAT-009, RBOT-OPS-005, REQ-AUTH-015, REQ-DEPLOYMENT-018
export const routes: Routes = withUnknownPath([
  {
    path: 'login',
    component: LoginPage,
    data: { heading: 'Запись к психологу', hint: 'Вход для клиентов по почте и паролю. Позже вход появится в Telegram.' },
  },
  { path: '', canMatch: [clientEntry('/login')], loadComponent: () => import('./home/home').then((m) => m.Home) },
  { path: 'book', canMatch: [clientEntry('/login')], loadComponent: () => import('./book/booking-wizard').then((m) => m.BookingWizard) },
], { notFound: NotFound });
