import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

interface NavItem {
  readonly label: string;
  readonly path: string;
  readonly icon: 'send' | 'list';
}

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
})
export class SidebarComponent {
  // One entry per feature route
  protected readonly navItems: readonly NavItem[] = [
    { label: 'Nuevo pago', path: '/payments/new', icon: 'send' },
    { label: 'Movimientos', path: '/movements', icon: 'list' },
  ];
}
