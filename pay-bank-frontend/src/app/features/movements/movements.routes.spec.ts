import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { routes } from '../../app.routes';
import { authInterceptor } from '../../core/interceptors/auth.interceptor';
import { AuthService } from '../../core/services/auth.service';

const api = environment.apiBaseUrl;

describe('movements route', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes, withComponentInputBinding()), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });

  afterEach(() => http.verify());

  it('shows the movements page to a user with a session', async () => {
    TestBed.inject(AuthService).login({ alias: 'ana', password: 'x' }).subscribe();
    http
      .expectOne(`${api}/api/auth/login`)
      .flush({ accessToken: 't', tokenType: 'Bearer', expiresIn: 900, alias: 'ana' });

    const navigation = harness.navigateByUrl('/movements?page=2');
    const req = await vi.waitFor(() => http.expectOne((r) => r.url === `${api}/api/accounts/me/movements`));
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.headers.get('Authorization')).toBe('Bearer t');
    req.flush({ content: [], page: 1, size: 20, totalElements: 0, totalPages: 0 });
    await navigation;

    expect(TestBed.inject(Router).url).toBe('/movements?page=2');
    await vi.waitFor(() =>
      expect((harness.routeNativeElement as HTMLElement).querySelector('app-movements-page')).toBeTruthy(),
    );
  });

  it('sends a user without a session to the login', async () => {
    const navigation = harness.navigateByUrl('/movements');
    (await vi.waitFor(() => http.expectOne(`${api}/api/auth/refresh`))).flush(
      {},
      { status: 401, statusText: 'Unauthorized' },
    );
    await navigation;

    expect(TestBed.inject(Router).url).toBe('/login?returnUrl=%2Fmovements');
  });
});
