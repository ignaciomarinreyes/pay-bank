import { Component, computed, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class HeaderComponent {
  private readonly authService = inject(AuthService);

  protected readonly currentUser = this.authService.currentUser;
  protected readonly initials = computed(() => (this.currentUser() ?? '').slice(0, 2).toUpperCase());

  protected logout(): void {
    this.authService.logout();
  }
}
