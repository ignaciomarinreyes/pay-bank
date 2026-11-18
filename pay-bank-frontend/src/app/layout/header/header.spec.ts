import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { AuthService } from '../../core/services/auth.service';
import { HeaderComponent } from './header';

describe('HeaderComponent', () => {
  const currentUser = signal<string | null>('ana');
  const logout = vi.fn();

  beforeEach(async () => {
    logout.mockClear();
    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [{ provide: AuthService, useValue: { currentUser, logout } }],
    }).compileComponents();
  });

  it('shows the alias of the current user', async () => {
    currentUser.set('ana');
    const fixture = TestBed.createComponent(HeaderComponent);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ana');
  });

  it('logs out when the button is pressed', async () => {
    currentUser.set('ana');
    const fixture = TestBed.createComponent(HeaderComponent);
    await fixture.whenStable();

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button')!.click();

    expect(logout).toHaveBeenCalledTimes(1);
  });

  it('hides the user area without a session', async () => {
    currentUser.set(null);
    const fixture = TestBed.createComponent(HeaderComponent);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('button')).toBeNull();
  });
});
