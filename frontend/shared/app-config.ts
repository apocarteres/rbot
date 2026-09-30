import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, Routes, withComponentInputBinding } from '@angular/router';
import { authInterceptor, provideAuth } from '@apocarteres/auth';
import { sanitisingInterceptor } from '@apocarteres/http';

// MVP-01, REQ-AUTH-015
export function appConfig(routes: Routes): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideZonelessChangeDetection(),
      provideRouter(routes, withComponentInputBinding()),
      provideHttpClient(withInterceptors([sanitisingInterceptor, authInterceptor])),
      provideAuth(),
    ],
  };
}
