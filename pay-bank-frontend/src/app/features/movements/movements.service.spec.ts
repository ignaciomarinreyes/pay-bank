import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { MovementsService } from './movements.service';

describe('MovementsService', () => {
  let service: MovementsService;
  let http: HttpTestingController;
  const url = `${environment.apiBaseUrl}/api/accounts/me/movements`;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(MovementsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests the first page with 20 elements', () => {
    const page = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
    let result: unknown;
    service.getMyMovements(0).subscribe((r) => (result = r));

    const req = http.expectOne((r) => r.url === url);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush(page);

    expect(result).toEqual(page);
  });

  it('sends the requested page', () => {
    service.getMyMovements(2).subscribe();

    expect(http.expectOne((r) => r.url === url).request.params.get('page')).toBe('2');
  });
});
