import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { AuthResponse } from '../services/auth.models';
import { AuthService } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

const api = environment.apiBaseUrl;
const apiUrl = `${api}/api/payments`;
const refreshUrl = `${api}/api/auth/refresh`;
const unauthorized = { status: 401, statusText: 'Unauthorized' };
const session = (token: string): AuthResponse => ({
  accessToken: token,
  tokenType: 'Bearer',
  expiresIn: 900,
  alias: 'ana',
});

describe('authInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'url', 'get').mockReturnValue('/movements');
    navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    // Open a session with token "t1"
    TestBed.inject(AuthService).login({ alias: 'ana', password: 'x' }).subscribe();
    http.expectOne(`${api}/api/auth/login`).flush(session('t1'));
  });

  afterEach(() => http.verify());

  it('adds the bearer token to API requests only', () => {
    client.get(apiUrl).subscribe();
    client.get('https://other.example.com/data').subscribe();

    expect(http.expectOne(apiUrl).request.headers.get('Authorization')).toBe('Bearer t1');
    expect(http.expectOne('https://other.example.com/data').request.headers.has('Authorization')).toBe(false);
  });

  it('does not add the token to /api/auth requests', () => {
    client.post(`${api}/api/auth/logout`, null).subscribe();

    expect(http.expectOne(`${api}/api/auth/logout`).request.headers.has('Authorization')).toBe(false);
  });

  it('renews the session on 401 and retries once with the new token', () => {
    let result: unknown;
    client.get(apiUrl).subscribe((r) => (result = r));

    http.expectOne(apiUrl).flush({}, unauthorized);
    http.expectOne(refreshUrl).flush(session('t2'));
    const retry = http.expectOne(apiUrl);
    expect(retry.request.headers.get('Authorization')).toBe('Bearer t2');
    retry.flush({ ok: true });

    expect(result).toEqual({ ok: true });
  });

  it('shares one renewal between concurrent 401 responses', () => {
    client.get(apiUrl).subscribe();
    client.get(apiUrl).subscribe();

    const first = http.match(apiUrl);
    expect(first).toHaveLength(2);
    first.forEach((r) => r.flush({}, unauthorized));

    http.expectOne(refreshUrl).flush(session('t2'));

    const retries = http.match(apiUrl);
    expect(retries).toHaveLength(2);
    retries.forEach((r) => {
      expect(r.request.headers.get('Authorization')).toBe('Bearer t2');
      r.flush({});
    });
  });

  it('propagates a 401 on the retry without renewing again', () => {
    let status: number | undefined;
    client.get(apiUrl).subscribe({ error: (e) => (status = e.status) });

    http.expectOne(apiUrl).flush({}, unauthorized);
    http.expectOne(refreshUrl).flush(session('t2'));
    http.expectOne(apiUrl).flush({}, unauthorized);

    expect(status).toBe(401);
    http.expectNone(refreshUrl);
  });

  it('goes to the login with the return url when the renewal fails', () => {
    let failed = false;
    client.get(apiUrl).subscribe({ error: () => (failed = true) });

    http.expectOne(apiUrl).flush({}, unauthorized);
    http.expectOne(refreshUrl).flush({}, unauthorized);

    expect(failed).toBe(true);
    expect(TestBed.inject(AuthService).isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/movements' } });
  });

  it('does not renew on errors other than 401', () => {
    let status: number | undefined;
    client.get(apiUrl).subscribe({ error: (e) => (status = e.status) });

    http.expectOne(apiUrl).flush({}, { status: 500, statusText: 'Server Error' });

    expect(status).toBe(500);
    http.expectNone(refreshUrl);
  });
});
