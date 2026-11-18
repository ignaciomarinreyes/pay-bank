import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { environment } from '../../../../environments/environment';
import { PageResponse } from '../../../shared/models/page-response';
import { MovementResponse } from '../movements.models';
import { MovementsPageComponent } from './movements-page';

const url = `${environment.apiBaseUrl}/api/accounts/me/movements`;

function movement(overrides: Partial<MovementResponse> = {}): MovementResponse {
  return {
    id: 1,
    createdAt: new Date(2026, 10, 18, 14, 5).toISOString(),
    type: 'CHARGE',
    amount: -1234.56,
    balanceAfter: 150,
    counterpartyAlias: 'maria',
    concept: 'Alquiler',
    ...overrides,
  };
}

function page(content: MovementResponse[], totalPages = 1, totalElements = content.length): PageResponse<MovementResponse> {
  return { content, page: 0, size: 20, totalElements, totalPages };
}

describe('MovementsPageComponent', () => {
  let fixture: ComponentFixture<MovementsPageComponent>;
  let http: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;
  let el: HTMLElement;

  beforeEach(async () => {
    registerLocaleData(localeEs);
    await TestBed.configureTestingModule({
      imports: [MovementsPageComponent],
      providers: [
        { provide: LOCALE_ID, useValue: 'es' },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(MovementsPageComponent);
    el = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  async function open(pageParam?: string): Promise<void> {
    if (pageParam !== undefined) {
      fixture.componentRef.setInput('page', pageParam);
    }
    await fixture.whenStable();
  }

  async function respond(body: PageResponse<MovementResponse>, expectedPage: string): Promise<void> {
    const req = http.expectOne((r) => r.url === url);
    expect(req.request.params.get('page')).toBe(expectedPage);
    expect(req.request.params.get('size')).toBe('20');
    req.flush(body);
    await fixture.whenStable();
  }

  const rows = () => Array.from(el.querySelectorAll('tbody tr'));
  const cells = (row: Element) => Array.from(row.querySelectorAll('td')).map((c) => c.textContent?.trim());

  it('requests the first page when there is no page param and shows a loading message', async () => {
    await open();
    expect(el.textContent).toContain('Cargando movimientos');

    await respond(page([movement()]), '0');
  });

  it.each([
    ['3', '2'],
    ['abc', '0'],
    ['0', '0'],
    ['-2', '0'],
  ])('page param "%s" requests API page %s', async (param, apiPage) => {
    await open(param);
    await respond(page([movement()], 5), apiPage);
  });

  it('shows 20 rows in the received order and the page indicator', async () => {
    const content = Array.from({ length: 20 }, (_, i) => movement({ id: i + 1, concept: `c${i}` }));
    await open();
    await respond(page(content, 3, 45), '0');

    expect(rows()).toHaveLength(20);
    expect(cells(rows()[0])[3]).toBe('c0');
    expect(cells(rows()[19])[3]).toBe('c19');
    expect(el.textContent).toContain('Página 1 de 3');
  });

  it('shows a charge with its negative amount, counterparty and concept', async () => {
    await open();
    await respond(page([movement()]), '0');

    expect(cells(rows()[0])).toEqual(['18/11/2026 14:05', 'Cargo', 'maria', 'Alquiler', '-1.234,56 €', '150,00 €']);
  });

  it('shows a credit with its positive amount', async () => {
    await open();
    await respond(page([movement({ type: 'CREDIT', amount: 50, balanceAfter: 120 })]), '0');

    expect(cells(rows()[0]).slice(1)).toEqual(['Abono', 'maria', 'Alquiler', '+50,00 €', '120,00 €']);
  });

  it('shows a dash when the movement has no concept', async () => {
    await open();
    await respond(page([movement({ concept: null })]), '0');

    expect(cells(rows()[0])[3]).toBe('—');
  });

  it('shows "No hay movimientos" without paginator when the account has none', async () => {
    await open();
    await respond(page([], 0, 0), '0');

    expect(el.textContent).toContain('No hay movimientos');
    expect(el.querySelector('app-paginator')).toBeNull();
    expect(el.querySelector('table')).toBeNull();
  });

  it('shows the error message when the request fails', async () => {
    await open();
    http.expectOne((r) => r.url === url).flush({}, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(el.querySelector('[role=alert]')?.textContent).toContain(
      'No se han podido cargar los movimientos. Inténtalo de nuevo más tarde.',
    );
    expect(el.querySelector('table')).toBeNull();
    expect(el.querySelector('app-paginator')).toBeNull();
  });

  it('shows "Página 3 de 3" with "Siguiente" disabled on the last page', async () => {
    await open('3');
    await respond(page([movement()], 3, 45), '2');

    expect(el.textContent).toContain('Página 3 de 3');
    const next = Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Siguiente'))!;
    expect(next.disabled).toBe(true);
  });

  it('navigates to the requested page through the query param', async () => {
    await open();
    await respond(page([movement()], 3, 45), '0');

    Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.includes('Siguiente'))!.click();

    expect(navigate).toHaveBeenCalledWith([], { queryParams: { page: 2 } });
  });

  it('replaces a page beyond the last one with the last page', async () => {
    await open('5');
    await respond(page([], 3, 45), '4');

    expect(navigate).toHaveBeenCalledWith([], { queryParams: { page: 3 }, replaceUrl: true });
  });
});
