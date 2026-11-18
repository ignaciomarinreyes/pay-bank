import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { AuthResponse } from './auth.models';
import { AuthService } from './auth.service';

const base = `${environment.apiBaseUrl}/api/auth`;
const response: AuthResponse = { accessToken: 't1', tokenType: 'Bearer', expiresIn: 900, alias: 'ana' };
const unauthorized = { status: 401, statusText: 'Unauthorized' };

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'url', 'get').mockReturnValue('/movements');
    navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  it('login stores the token and the alias', () => {
    service.login({ alias: 'ana', password: 'x' }).subscribe();

    const req = http.expectOne(`${base}/login`);
    expect(req.request.withCredentials).toBe(true);
    expect(req.request.body).toEqual({ alias: 'ana', password: 'x' });
    req.flush(response);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.currentUser()).toBe('ana');
    expect(service.accessTokenValue()).toBe('t1');
  });

  it('login failure leaves the session closed', () => {
    service.login({ alias: 'ana', password: 'bad' }).subscribe({ error: () => undefined });

    http.expectOne(`${base}/login`).flush({}, unauthorized);

    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
  });

  it('concurrent refresh calls share a single request', () => {
    const tokens: string[] = [];
    service.refresh().subscribe((t) => tokens.push(t));
    service.refresh().subscribe((t) => tokens.push(t));

    const req = http.expectOne(`${base}/refresh`);
    expect(req.request.withCredentials).toBe(true);
    req.flush(response);

    expect(tokens).toEqual(['t1', 't1']);
  });

  it('a failed refresh clears the session and goes to the login with the current url', () => {
    service.refresh().subscribe({ error: () => undefined });

    http.expectOne(`${base}/refresh`).flush({}, unauthorized);

    expect(service.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/movements' } });
  });

  it('ensureSession is true without requests when there is a token', () => {
    service.login({ alias: 'ana', password: 'x' }).subscribe();
    http.expectOne(`${base}/login`).flush(response);

    let result: boolean | undefined;
    service.ensureSession().subscribe((v) => (result = v));

    expect(result).toBe(true);
    http.expectNone(`${base}/refresh`);
  });

  it('ensureSession restores the session with one refresh', () => {
    let result: boolean | undefined;
    service.ensureSession().subscribe((v) => (result = v));

    http.expectOne(`${base}/refresh`).flush(response);

    expect(result).toBe(true);
    expect(service.currentUser()).toBe('ana');
  });

  it('ensureSession is false when the refresh fails and does not navigate by itself', () => {
    let result: boolean | undefined;
    service.ensureSession().subscribe((v) => (result = v));

    http.expectOne(`${base}/refresh`).flush({}, unauthorized);

    expect(result).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('ensureSession does not retry the refresh after a failed restore until a new login', () => {
    service.ensureSession().subscribe();
    http.expectOne(`${base}/refresh`).flush({}, unauthorized);

    let result: boolean | undefined;
    service.ensureSession().subscribe((v) => (result = v));
    expect(result).toBe(false);
    http.expectNone(`${base}/refresh`);

    service.login({ alias: 'ana', password: 'x' }).subscribe();
    http.expectOne(`${base}/login`).flush(response);
    service.clearSession();
    service.ensureSession().subscribe((v) => (result = v));
    http.expectNone(`${base}/refresh`);
    expect(result).toBe(false);
  });

  it('logout calls the backend, clears the session and goes to the login', () => {
    service.login({ alias: 'ana', password: 'x' }).subscribe();
    http.expectOne(`${base}/login`).flush(response);

    service.logout();
    const req = http.expectOne(`${base}/logout`);
    expect(req.request.withCredentials).toBe(true);
    req.flush(null, { status: 204, statusText: 'No Content' });

    expect(service.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('logout clears the session even if the backend fails', () => {
    service.login({ alias: 'ana', password: 'x' }).subscribe();
    http.expectOne(`${base}/login`).flush(response);

    service.logout();
    http.expectOne(`${base}/logout`).flush({}, { status: 500, statusText: 'Server Error' });

    expect(service.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
