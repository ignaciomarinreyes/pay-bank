import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AccountResponse } from './account.models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);

  getMyAccount(): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(`${environment.apiBaseUrl}/api/accounts/me`);
  }
}
