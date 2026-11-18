/** Body of `POST /api/payments` (see `CreatePaymentRequest` in the backend OpenAPI contract). */
export interface CreatePaymentRequest {
  recipientAlias: string;
  /** Amount in euros, greater than 0 with at most 2 decimals. */
  amount: number;
  /** Optional, at most 140 characters. */
  concept: string | null;
}

/** Response of `POST /api/payments` (`PaymentResponse`). */
export interface PaymentResponse {
  id: number;
  recipientAlias: string;
  amount: number;
  concept: string | null;
  /** ISO-8601 date-time (UTC). */
  createdAt: string;
  /** Balance of the payer's account after the payment. */
  balanceAfter: number;
}

/** Business error codes of the `ProblemDetail` returned with 422. */
export type PaymentErrorCode = 'RECIPIENT_NOT_FOUND' | 'SELF_PAYMENT' | 'INSUFFICIENT_BALANCE';
