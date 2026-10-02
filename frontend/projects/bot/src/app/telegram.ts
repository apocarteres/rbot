import { HttpInterceptorFn } from '@angular/common/http';
import { CanMatchFn } from '@angular/router';
import { signedIn } from '@apocarteres/auth';

const KEY = 'rbot.telegram.init-data';
const HEADER = 'X-Telegram-Init-Data';

function launchData(): string {
  const launched = new URLSearchParams(location.hash.replace(/^#/, '')).get('tgWebAppData') ?? '';
  const fromHash = launched.includes('hash=') ? launched : '';
  if (launched) {
    history.replaceState(history.state, '', location.pathname + location.search);
  }
  try {
    if (fromHash) {
      sessionStorage.setItem(KEY, fromHash);
      return fromHash;
    }
    return sessionStorage.getItem(KEY) ?? '';
  } catch {
    return fromHash;
  }
}

const INIT_DATA = launchData();

const INVITE_KEY = 'rbot.telegram.invite';

function launchInvite(): string {
  const fromQuery = new URLSearchParams(location.search).get('invite') ?? '';
  if (fromQuery) {
    history.replaceState(history.state, '', location.pathname);
  }
  try {
    if (fromQuery) {
      sessionStorage.setItem(INVITE_KEY, fromQuery);
      return fromQuery;
    }
    return sessionStorage.getItem(INVITE_KEY) ?? '';
  } catch {
    return fromQuery;
  }
}

let invite = INIT_DATA ? launchInvite() : '';

// RBOT-FEAT-018, ADR-0002
export function pendingInvite(): string {
  return invite;
}

// RBOT-FEAT-018, ADR-0002
export function inviteSettled(): void {
  invite = '';
  try {
    sessionStorage.removeItem(INVITE_KEY);
  } catch {
    invite = '';
  }
}

// MVP-08, RBOT-FEAT-009, ADR-0002
export function insideTelegram(): boolean {
  return INIT_DATA !== '';
}

// MVP-08, RBOT-FEAT-009, ADR-0002
export const telegramInitDataInterceptor: HttpInterceptorFn = (request, next) =>
  insideTelegram() && request.url.startsWith('/api/miniapp') ? next(request.clone({ setHeaders: { [HEADER]: INIT_DATA } })) : next(request);

// MVP-08, RBOT-FEAT-009
export function clientEntry(redirect: string): CanMatchFn {
  const guest = signedIn(redirect);
  return (...context) => insideTelegram() || guest(...context);
}
