import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { appUpdateInterceptor, provideAppUpdate } from '@apocarteres/app-update';
import { authInterceptor, provideAuth } from '@apocarteres/auth';
import { provideNotifications } from '@apocarteres/notifications';
import { SESSION_EXPIRED, sanitisingInterceptor, sessionExpiredInterceptor } from '@apocarteres/http';
import { API_VERSION } from '../../../../shared/api-version';
import { AppClock } from '../../../../shared/clock';
import { sessionExpired } from '../../../../shared/session-expired';
import { SystemClock } from '../../../../shared/system-clock';
import { UpdateAvailable } from '../../../../shared/update-available';
import { UpdateRequired } from '../../../../shared/update-required';
import { routes } from './app.routes';

// MVP-01, MVP-02, RBOT-API-001, REQ-TYPESCRIPT-CLOCK-002, REQ-AUTH-015, REQ-API-002, REQ-CLIENT-UPDATE-001, REQ-CLIENT-UPDATE-006, RBOT-FEAT-020
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([sanitisingInterceptor, sessionExpiredInterceptor, appUpdateInterceptor, authInterceptor])),
    provideAppUpdate({ apiVersion: API_VERSION, available: UpdateAvailable, required: UpdateRequired }),
    provideAuth(),
    provideNotifications({ limit: 10 }),
    { provide: SESSION_EXPIRED, useFactory: sessionExpired },
    { provide: AppClock, useClass: SystemClock },
  ],
};
