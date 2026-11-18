import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Observable, firstValueFrom, of } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';
import { guestGuard } from './guest.guard';

function setup(hasSession: boolean): void {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AuthService, useValue: { ensureSession: () => of(hasSession) } }],
  });
}

function run(guard: CanActivateFn, url: string): Promise<boolean | UrlTree> {
  const result = TestBed.runInInjectionContext(() =>
    guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
  );
  return firstValueFrom(result as Observable<boolean | UrlTree>);
}

describe('authGuard', () => {
  it('allows access with a session', async () => {
    setup(true);
    expect(await run(authGuard, '/movements')).toBe(true);
  });

  it('redirects to the login with the requested url when there is no session', async () => {
    setup(false);
    const result = await run(authGuard, '/movements');
    expect(result).toBeInstanceOf(UrlTree);
    expect(result.toString()).toBe('/login?returnUrl=%2Fmovements');
  });
});

describe('guestGuard', () => {
  it('allows the login page without a session', async () => {
    setup(false);
    expect(await run(guestGuard, '/login')).toBe(true);
  });

  it('redirects to the root with a session', async () => {
    setup(true);
    const result = await run(guestGuard, '/login');
    expect(result).toBeInstanceOf(UrlTree);
    expect(result.toString()).toBe('/');
  });
});
