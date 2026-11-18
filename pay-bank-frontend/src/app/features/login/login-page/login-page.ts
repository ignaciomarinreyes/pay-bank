import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPageComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  /** Protected URL the user tried to open before being sent to the login (query param). */
  readonly returnUrl = input<string | undefined>();

  protected readonly form = new FormGroup({
    alias: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });
  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigateByUrl(this.targetUrl()),
      error: (error: unknown) => {
        this.submitting.set(false);
        this.errorMessage.set(
          error instanceof HttpErrorResponse && error.status === 401
            ? 'Alias o contraseña incorrectos'
            : 'No se ha podido iniciar sesión. Inténtalo de nuevo más tarde.',
        );
      },
    });
  }

  /** Only internal paths are accepted, to avoid open redirects. */
  private targetUrl(): string {
    const url = this.returnUrl();
    const isInternal = !!url && url.startsWith('/') && !url.startsWith('//');
    const isLogin = !!url && (url === '/login' || url.startsWith('/login?') || url.startsWith('/login/'));
    return isInternal && !isLogin ? url : '/';
  }
}
