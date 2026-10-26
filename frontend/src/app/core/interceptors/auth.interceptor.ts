import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

/** Adds the bearer token to API calls and drops the session when the API says it's no longer valid. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const isApi = req.url.startsWith(environment.apiUrl);
  const isAuthCall = req.url.startsWith(`${environment.apiUrl}/auth/`);

  const request = token && isApi && !isAuthCall ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(request).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401 && token && !isAuthCall) {
        auth.clear();
      }
      return throwError(() => err);
    }),
  );
};
