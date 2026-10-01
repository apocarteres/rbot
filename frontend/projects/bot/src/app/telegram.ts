import { HttpInterceptorFn } from '@angular/common/http';
import { CanMatchFn } from '@angular/router';
import { signedIn } from '@apocarteres/auth';

const KEY = 'rbot.telegram.init-data';
const HEADER = 'X-Telegram-Init-Data';

function launchData(): string {
  const fromHash = new URLSearchParams(location.hash.replace(/^#/, '')).get('tgWebAppData') ?? '';
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
