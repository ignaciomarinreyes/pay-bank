import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-header',
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class HeaderComponent {
  private readonly authService = inject(AuthService);

  protected readonly currentUser = this.authService.currentUser;

  protected logout(): void {
    this.authService.logout();
  }
}
