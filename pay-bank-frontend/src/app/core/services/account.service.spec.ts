import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AccountService } from './account.service';

describe('AccountService', () => {
  it('gets the account of the current user', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(AccountService);
    const http = TestBed.inject(HttpTestingController);
    let result: unknown;

    service.getMyAccount().subscribe((r) => (result = r));
    const req = http.expectOne(`${environment.apiBaseUrl}/api/accounts/me`);
    expect(req.request.method).toBe('GET');
    req.flush({ balance: 1015 });

    expect(result).toEqual({ balance: 1015 });
    http.verify();
  });
});
