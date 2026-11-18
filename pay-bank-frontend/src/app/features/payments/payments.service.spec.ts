import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { PaymentResponse } from './payments.models';
import { PaymentsService } from './payments.service';

describe('PaymentsService', () => {
  it('posts the payment and returns the response', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(PaymentsService);
    const http = TestBed.inject(HttpTestingController);
    const response: PaymentResponse = {
      id: 1,
      recipientAlias: 'maria',
      amount: 250,
      concept: 'Cena',
      createdAt: '2026-11-18T10:00:00Z',
      balanceAfter: 750,
    };
    let result: unknown;

    service.createPayment({ recipientAlias: 'Maria', amount: 250, concept: 'Cena' }).subscribe((r) => (result = r));
    const req = http.expectOne(`${environment.apiBaseUrl}/api/payments`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ recipientAlias: 'Maria', amount: 250, concept: 'Cena' });
    req.flush(response, { status: 201, statusText: 'Created' });

    expect(result).toEqual(response);
    http.verify();
  });
});
