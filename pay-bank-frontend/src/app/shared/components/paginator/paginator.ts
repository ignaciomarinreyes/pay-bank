import { Component, input, output } from '@angular/core';

/** Previous/next paginator. `page` is one-based. */
@Component({
  selector: 'app-paginator',
  templateUrl: './paginator.html',
  styleUrl: './paginator.scss',
})
export class PaginatorComponent {
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly pageChange = output<number>();
}
