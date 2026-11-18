import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { vi } from 'vitest';
import { environment } from '../environments/environment';
import { routes } from './app.routes';
import { AuthService } from './core/services/auth.service';

@Component({ template: '<p>dummy</p>' })
class DummyComponent {}

const api = `${environment.apiBaseUrl}/api/auth`;
const session = { accessToken: 't', tokenType: 'Bearer', expiresIn: 900, alias: 'ana' };
const unauthorized = { status: 401, statusText: 'Unauthorized' };

// Real routes, with test children under the shell (the dashboard and movements do not exist yet)
const testRoutes = routes.map((route) =>
  route.path === ''
    ? {
        ...route,
        children: [
          { path: '', component: DummyComponent },
          { path: 'movements', component: DummyComponent },
        ],
      }
    : route,
);

describe('app routes', () => {
  let http: HttpTestingController;
  let router: Router;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(testRoutes, withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  const flushRefresh = (body: object, opts?: { status: number; statusText: string }) =>
    vi.waitFor(() => http.expectOne(`${api}/refresh`)).then((req) => req.flush(body, opts));

  it('sends an anonymous user to the login with the requested url and back after logging in', async () => {
    const navigation = harness.navigateByUrl('/movements');
    await flushRefresh({}, unauthorized);
    await navigation;
    expect(router.url).toBe('/login?returnUrl=%2Fmovements');

    const el = harness.routeNativeElement as HTMLElement;
    const alias = el.querySelector<HTMLInputElement>('input[name=alias]')!;
    const password = el.querySelector<HTMLInputElement>('input[name=password]')!;
    alias.value = 'ana';
    alias.dispatchEvent(new Event('input'));
    password.value = 'x';
    password.dispatchEvent(new Event('input'));
    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));

    (await vi.waitFor(() => http.expectOne(`${api}/login`))).flush(session);
    await vi.waitFor(() => expect(router.url).toBe('/movements'));
  });

  it('shows the login page for a user without session going straight to it', async () => {
    const navigation = harness.navigateByUrl('/login');
    await flushRefresh({}, unauthorized);
    await navigation;

    expect(router.url).toBe('/login');
    expect(harness.routeNativeElement?.querySelector('form')).toBeTruthy();
  });

  it('restores the session from the refresh cookie without going through the login', async () => {
    const navigation = harness.navigateByUrl('/');
    await flushRefresh(session);
    await navigation;

    expect(router.url).toBe('/');
    expect(TestBed.inject(AuthService).currentUser()).toBe('ana');
  });

  it('sends a user with a session away from the login to the root', async () => {
    TestBed.inject(AuthService).login({ alias: 'ana', password: 'x' }).subscribe();
    http.expectOne(`${api}/login`).flush(session);

    await harness.navigateByUrl('/login');

    expect(router.url).toBe('/');
  });

  it('requires a new login after logging out', async () => {
    const auth = TestBed.inject(AuthService);
    auth.login({ alias: 'ana', password: 'x' }).subscribe();
    http.expectOne(`${api}/login`).flush(session);
    await harness.navigateByUrl('/');

    auth.logout();
    http.expectOne(`${api}/logout`).flush(null, { status: 204, statusText: 'No Content' });
    await vi.waitFor(() => expect(router.url).toBe('/login'));

    // No session and no cookie to restore it from: no new refresh request is made
    await harness.navigateByUrl('/');
    expect(router.url).toBe('/login?returnUrl=%2F');
  });
});
