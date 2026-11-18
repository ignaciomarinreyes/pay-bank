import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    // The authentication guard (core/guards) will protect every route under the shell
    loadComponent: () => import('./layout/shell/shell').then((m) => m.ShellComponent),
    // Feature routes (lazy loaded from features/) are registered here
    children: [],
  },
  { path: '**', redirectTo: '' },
];
