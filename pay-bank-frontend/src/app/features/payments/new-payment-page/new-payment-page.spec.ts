import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { environment } from '../../../../environments/environment';
import { NewPaymentPageComponent } from './new-payment-page';

const accountUrl = `${environment.apiBaseUrl}/api/accounts/me`;
const paymentsUrl = `${environment.apiBaseUrl}/api/payments`;
const created = {
  id: 1,
  recipientAlias: 'maria',
  amount: 250,
  concept: 'Cena',
  createdAt: '2026-11-18T10:00:00Z',
  balanceAfter: 750,
};
const AMOUNT_MESSAGE = 'Introduce un importe mayor que 0 con un máximo de 2 decimales';

describe('NewPaymentPageComponent', () => {
  let fixture: ComponentFixture<NewPaymentPageComponent>;
  let http: HttpTestingController;
  let navigateByUrl: ReturnType<typeof vi.spyOn>;
  let el: HTMLElement;

  beforeEach(async () => {
    registerLocaleData(localeEs);
    await TestBed.configureTestingModule({
      imports: [NewPaymentPageComponent],
      providers: [
        { provide: LOCALE_ID, useValue: 'es' },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(NewPaymentPageComponent);
    el = fixture.nativeElement as HTMLElement;
  });

  afterEach(() => http.verify());

  async function loadBalance(balance = 1015): Promise<void> {
    http.expectOne(accountUrl).flush({ balance });
    await fixture.whenStable();
  }

  function type(name: string, value: string): void {
    const input = el.querySelector<HTMLInputElement>(`input[name=${name}]`)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function fill(recipient: string, amount: string, concept = ''): void {
    type('recipientAlias', recipient);
    type('amount', amount);
    type('concept', concept);
  }

  async function submit(): Promise<void> {
    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  const payRequest = (): TestRequest => http.expectOne(paymentsUrl);
  const value = (name: string) => el.querySelector<HTMLInputElement>(`input[name=${name}]`)!.value;

  // CA-01
  it('shows the three fields and the pay button', async () => {
    await loadBalance();

    expect(el.querySelector('input[name=recipientAlias]')).toBeTruthy();
    expect(el.querySelector('input[name=amount]')).toBeTruthy();
    expect(el.querySelector('input[name=concept]')).toBeTruthy();
    expect(el.querySelector('button[type=submit]')?.textContent).toContain('Pagar');
  });

  // CA-02
  it('shows the available balance', async () => {
    await loadBalance(1015);

    expect(el.textContent).toContain('Saldo disponible: 1.015,00 €');
  });

  it('shows the form without the balance line when the balance cannot be loaded', async () => {
    http.expectOne(accountUrl).flush({}, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(el.textContent).not.toContain('Saldo disponible');
    expect(el.querySelector('form')).toBeTruthy();
  });

  // CA-03
  it.each([
    ['', '', ['El destinatario es obligatorio', 'El importe es obligatorio']],
    ['', '10', ['El destinatario es obligatorio']],
    ['maria', '', ['El importe es obligatorio']],
  ])('does not send recipient "%s" with amount "%s"', async (recipient, amount, messages) => {
    await loadBalance();
    fill(recipient, amount);
    await submit();

    http.expectNone(paymentsUrl);
    for (const message of messages) {
      expect(el.textContent).toContain(message);
    }
  });

  // CA-04
  it.each(['0', '-5', '10,999', 'abc', '0,00', '1.234,50'])('rejects the amount "%s"', async (amount) => {
    await loadBalance();
    fill('maria', amount);
    await submit();

    http.expectNone(paymentsUrl);
    expect(el.textContent).toContain(AMOUNT_MESSAGE);
  });

  // CA-05
  it.each([
    ['12,50', 12.5],
    ['12.50', 12.5],
    ['7', 7],
    ['0,01', 0.01],
  ])('sends the amount "%s" as %s', async (text, amount) => {
    await loadBalance();
    fill('maria', text);
    await submit();

    expect(payRequest().request.body.amount).toBe(amount);
  });

  // CA-06
  it('rejects a concept of 141 characters and accepts one of 140', async () => {
    await loadBalance();
    fill('maria', '10', 'x'.repeat(141));
    await submit();

    http.expectNone(paymentsUrl);
    expect(el.textContent).toContain('El concepto no puede superar los 140 caracteres');

    type('concept', 'x'.repeat(140));
    await submit();
    expect(payRequest().request.body.concept).toBe('x'.repeat(140));
  });

  // CA-11
  it('sends the payment and goes to the movements', async () => {
    await loadBalance();
    fill('Maria', '250,00', 'Cena');
    await submit();

    const req = payRequest();
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ recipientAlias: 'Maria', amount: 250, concept: 'Cena' });
    req.flush(created, { status: 201, statusText: 'Created' });
    await fixture.whenStable();

    expect(navigateByUrl).toHaveBeenCalledWith('/movements');
  });

  // CA-12
  it.each(['', '   '])('sends a blank concept "%s" as null', async (concept) => {
    await loadBalance();
    fill('maria', '10', concept);
    await submit();

    expect(payRequest().request.body.concept).toBeNull();
  });

  // CA-07, CA-08, CA-09, CA-13
  it.each([
    ['RECIPIENT_NOT_FOUND', 'No existe ningún usuario con ese alias'],
    ['SELF_PAYMENT', 'No puedes hacerte un pago a ti mismo'],
    ['INSUFFICIENT_BALANCE', 'Saldo insuficiente'],
  ])('shows the message of %s and keeps the data', async (code, message) => {
    await loadBalance();
    fill('maria', '10,50', 'Cena');
    await submit();
    payRequest().flush({ status: 422, code }, { status: 422, statusText: 'Unprocessable Entity' });
    await fixture.whenStable();

    expect(el.querySelector('[role=alert]')?.textContent).toContain(message);
    expect(navigateByUrl).not.toHaveBeenCalled();
    expect([value('recipientAlias'), value('amount'), value('concept')]).toEqual(['maria', '10,50', 'Cena']);
    expect(el.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(false);
  });

  // CA-18
  it.each([
    [500, {}],
    [400, { code: 'VALIDATION_ERROR' }],
    [422, { code: 'ACCOUNT_NOT_FOUND' }],
  ])('shows the generic message on a %s', async (status, body) => {
    await loadBalance();
    fill('maria', '10', 'Cena');
    await submit();
    payRequest().flush(body, { status, statusText: 'Error' });
    await fixture.whenStable();

    expect(el.querySelector('[role=alert]')?.textContent).toContain(
      'No se ha podido realizar el pago. Inténtalo de nuevo más tarde.',
    );
    expect([value('recipientAlias'), value('amount'), value('concept')]).toEqual(['maria', '10', 'Cena']);
  });

  // CA-14
  it('disables the button while paying and sends only one payment', async () => {
    await loadBalance();
    fill('maria', '10');
    await submit();

    expect(el.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(true);
    await submit();
    await submit();

    expect(http.match(paymentsUrl)).toHaveLength(1);
  });
});
