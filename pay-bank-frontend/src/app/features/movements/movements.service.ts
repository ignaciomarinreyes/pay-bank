import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../../shared/models/page-response';
import { MovementResponse } from './movements.models';

export const MOVEMENTS_PAGE_SIZE = 20;

@Injectable({ providedIn: 'root' })
export class MovementsService {
  private readonly http = inject(HttpClient);

  /** `page` is zero-based, as in the API. */
  getMyMovements(page: number): Observable<PageResponse<MovementResponse>> {
    const params = new HttpParams().set('page', page).set('size', MOVEMENTS_PAGE_SIZE);
    return this.http.get<PageResponse<MovementResponse>>(
      `${environment.apiBaseUrl}/api/accounts/me/movements`,
      { params },
    );
  }
}
