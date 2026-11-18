import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { environment } from '../../../../environments/environment';
import { LoginPageComponent } from './login-page';

const loginUrl = `${environment.apiBaseUrl}/api/auth/login`;
const session = { accessToken: 't', tokenType: 'Bearer', expiresIn: 900, alias: 'ana' };

describe('LoginPageComponent', () => {
  let fixture: ComponentFixture<LoginPageComponent>;
  let http: HttpTestingController;
  let navigateByUrl: ReturnType<typeof vi.spyOn>;
  let el: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(LoginPageComponent);
    el = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function fill(alias: string, password: string): void {
    const aliasInput = el.querySelector<HTMLInputElement>('input[name=alias]')!;
    const passwordInput = el.querySelector<HTMLInputElement>('input[name=password]')!;
    aliasInput.value = alias;
    aliasInput.dispatchEvent(new Event('input'));
    passwordInput.value = password;
    passwordInput.dispatchEvent(new Event('input'));
  }

  async function submit(): Promise<void> {
    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  it('shows the alias field, a hidden password field and the submit button', () => {
    expect(el.querySelector('input[name=alias]')).toBeTruthy();
    expect(el.querySelector('input[name=password]')?.getAttribute('type')).toBe('password');
    expect(el.querySelector('button[type=submit]')?.textContent).toContain('Iniciar sesión');
  });

  it.each([
    ['', '', ['El alias es obligatorio', 'La contraseña es obligatoria']],
    ['', 'x', ['El alias es obligatorio']],
    ['ana', '', ['La contraseña es obligatoria']],
  ])('does not send the form with alias "%s" and password "%s"', async (alias, password, messages) => {
    fill(alias, password);
    await submit();

    http.expectNone(loginUrl);
    for (const message of messages) {
      expect(el.textContent).toContain(message);
    }
  });

  it('shows the generic credentials message on a 401', async () => {
    fill('ana', 'bad');
    await submit();
    http.expectOne(loginUrl).flush({}, { status: 401, statusText: 'Unauthorized' });
    await fixture.whenStable();

    expect(el.querySelector('[role=alert]')?.textContent).toContain('Alias o contraseña incorrectos');
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('shows a generic message on server errors', async () => {
    fill('ana', 'x');
    await submit();
    http.expectOne(loginUrl).flush({}, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(el.querySelector('[role=alert]')?.textContent).toContain('No se ha podido iniciar sesión');
  });

  it('disables the button while the request is in progress', async () => {
    fill('ana', 'x');
    await submit();

    expect(el.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(true);
    http.expectOne(loginUrl).flush(session);
  });

  it('goes to the root after a successful login without return url', async () => {
    fill('ana', 'x');
    await submit();
    http.expectOne(loginUrl).flush(session);

    expect(navigateByUrl).toHaveBeenCalledWith('/');
  });

  it.each([
    ['/movements', '/movements'],
    ['//evil.com', '/'],
    ['https://evil.com', '/'],
    ['/login', '/'],
    ['/login?returnUrl=%2Fx', '/'],
  ])('uses return url "%s" as "%s"', async (returnUrl, expected) => {
    fixture.componentRef.setInput('returnUrl', returnUrl);
    fill('ana', 'x');
    await submit();
    http.expectOne(loginUrl).flush(session);

    expect(navigateByUrl).toHaveBeenCalledWith(expected);
  });
});
