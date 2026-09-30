import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { appUpdateInterceptor, provideAppUpdate } from '@apocarteres/app-update';
import { authInterceptor, provideAuth } from '@apocarteres/auth';
import { sanitisingInterceptor } from '@apocarteres/http';
import { API_VERSION } from '../../../../shared/api-version';
import { UpdateAvailable } from '../../../../shared/update-available';
import { UpdateRequired } from '../../../../shared/update-required';
import { routes } from './app.routes';

// MVP-01, REQ-AUTH-015, REQ-CLIENT-UPDATE-001, REQ-CLIENT-UPDATE-006
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([sanitisingInterceptor, appUpdateInterceptor, authInterceptor])),
    provideAppUpdate({ apiVersion: API_VERSION, available: UpdateAvailable, required: UpdateRequired }),
    provideAuth(),
  ],
};
