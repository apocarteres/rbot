import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { SessionExpiry } from '@apocarteres/http';

const AUTHENTICATION_REQUIRED = 'authentication-required';

// RBOT-API-001, REQ-API-002, REQ-AUTH-008
export function sessionExpired(): (expiry: SessionExpiry) => void {
  const router = inject(Router);
  return (expiry) => {
    if (expiry.problem?.code === AUTHENTICATION_REQUIRED) {
      void router.navigateByUrl('/login');
    }
  };
}
