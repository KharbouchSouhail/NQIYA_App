import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.isLoggedIn() ? true : inject(Router).createUrlTree(['/login'], { queryParams: { next: state.url } });
};

/** Waits for the profile on a cold deep link, since the role lives in it. */
export const adminGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn() && !auth.me()) {
    await auth.restore();
  }
  return auth.isAdmin() ? true : router.createUrlTree(['/map']);
};
