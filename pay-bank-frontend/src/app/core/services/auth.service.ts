import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest } from './auth.models';

/**
 * Session state and operations. The access token lives only in memory; the refresh token is an
 * HttpOnly cookie managed by the browser, so it is never read from JavaScript.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/auth`;

  private readonly accessToken = signal<string | null>(null);
  private readonly alias = signal<string | null>(null);
  private renewal$: Observable<string> | null = null;
  /** True once we know the refresh cookie is not usable, so guards do not retry until a new login. */
  private restoreFailed = false;

  readonly currentUser = this.alias.asReadonly();
  readonly isAuthenticated = computed(() => this.accessToken() !== null);

  login(request: LoginRequest): Observable<void> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/login`, request, { withCredentials: true })
      .pipe(
        tap((response) => this.setSession(response)),
        map(() => undefined),
      );
  }

  /**
   * Renews the session. If the renewal fails the session is cleared and the user is sent to the
   * login page. Concurrent calls share a single request.
   */
  refresh(): Observable<string> {
    return this.renew().pipe(
      catchError((error: unknown) => {
        this.redirectToLogin();
        return throwError(() => error);
      }),
    );
  }

  /** True when there is a session; otherwise tries once to restore it from the refresh cookie. */
  ensureSession(): Observable<boolean> {
    if (this.accessToken() !== null) {
      return of(true);
    }
    if (this.restoreFailed) {
      return of(false);
    }
    return this.renew().pipe(
      map(() => true),
      catchError(() => of(false)),
    );
  }

  logout(): void {
    this.http
      .post<void>(`${this.baseUrl}/logout`, null, { withCredentials: true })
      .pipe(catchError(() => of(undefined)))
      .subscribe(() => {
        this.clearSession();
        void this.router.navigate(['/login']);
      });
  }

  accessTokenValue(): string | null {
    return this.accessToken();
  }

  clearSession(): void {
    this.accessToken.set(null);
    this.alias.set(null);
    this.restoreFailed = true;
  }

  private renew(): Observable<string> {
    if (!this.renewal$) {
      this.renewal$ = this.http
        .post<AuthResponse>(`${this.baseUrl}/refresh`, null, { withCredentials: true })
        .pipe(
          tap((response) => this.setSession(response)),
          map((response) => response.accessToken),
          catchError((error: unknown) => {
            this.clearSession();
            return throwError(() => error);
          }),
          finalize(() => (this.renewal$ = null)),
          shareReplay({ bufferSize: 1, refCount: false }),
        );
    }
    return this.renewal$;
  }

  private setSession(response: AuthResponse): void {
    this.accessToken.set(response.accessToken);
    this.alias.set(response.alias);
    this.restoreFailed = false;
  }

  private redirectToLogin(): void {
    const url = this.router.url;
    if (url.startsWith('/login')) {
      return;
    }
    void this.router.navigate(['/login'], { queryParams: { returnUrl: url } });
  }
}
