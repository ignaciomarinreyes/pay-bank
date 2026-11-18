import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

/**
 * Adds the access token to API requests. On a 401 it renews the session once and retries the
 * request with the new token.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  const isApiRequest = req.url.startsWith(environment.apiBaseUrl);
  const isAuthRequest = req.url.startsWith(`${environment.apiBaseUrl}/api/auth/`);
  if (!isApiRequest || isAuthRequest) {
    return next(req);
  }

  const withToken = (request: HttpRequest<unknown>, token: string) =>
    request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

  const token = authService.accessTokenValue();
  return next(token ? withToken(req, token) : req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        return authService.refresh().pipe(switchMap((newToken) => next(withToken(req, newToken))));
      }
      return throwError(() => error);
    }),
  );
};
