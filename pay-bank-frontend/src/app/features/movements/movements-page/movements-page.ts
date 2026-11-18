import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, untracked } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { catchError, map, of, startWith, switchMap } from 'rxjs';
import { PageTitleComponent } from '../../../shared/components/page-title/page-title';
import { PaginatorComponent } from '../../../shared/components/paginator/paginator';
import { PageResponse } from '../../../shared/models/page-response';
import { AmountPipe } from '../../../shared/pipes/amount.pipe';
import { MovementResponse } from '../movements.models';
import { MovementsService } from '../movements.service';

type MovementsState =
  | { status: 'loading' }
  | { status: 'error' }
  | { status: 'loaded'; data: PageResponse<MovementResponse> };

@Component({
  selector: 'app-movements-page',
  imports: [DatePipe, AmountPipe, PageTitleComponent, PaginatorComponent],
  templateUrl: './movements-page.html',
  styleUrl: './movements-page.scss',
})
export class MovementsPageComponent {
  private readonly movementsService = inject(MovementsService);
  private readonly router = inject(Router);

  /** One-based page number from the `page` query param (bound by the router). */
  readonly page = input<string | undefined>();

  protected readonly currentPage = computed(() => {
    const raw = this.page();
    const value = raw !== undefined && /^\d+$/.test(raw) ? Number(raw) : 1;
    return value >= 1 ? value : 1;
  });

  protected readonly state = toSignal(
    toObservable(this.currentPage).pipe(
      switchMap((page) =>
        this.movementsService.getMyMovements(page - 1).pipe(
          map((data): MovementsState => ({ status: 'loaded', data })),
          catchError(() => of<MovementsState>({ status: 'error' })),
          startWith<MovementsState>({ status: 'loading' }),
        ),
      ),
    ),
    { initialValue: { status: 'loading' } as MovementsState },
  );

  constructor() {
    // A page beyond the last one is replaced by the last page
    effect(() => {
      const state = this.state();
      const page = this.currentPage();
      if (state.status === 'loaded' && state.data.totalPages > 0 && page > state.data.totalPages) {
        untracked(() => this.goTo(state.data.totalPages, true));
      }
    });
  }

  protected goTo(page: number, replaceUrl = false): void {
    void this.router.navigate([], { queryParams: { page }, ...(replaceUrl ? { replaceUrl } : {}) });
  }
}
