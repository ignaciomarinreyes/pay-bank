export type MovementType = 'CHARGE' | 'CREDIT';

/** Movement of the account (see `MovementResponse` in the backend OpenAPI contract). */
export interface MovementResponse {
  id: number;
  /** ISO-8601 date-time (UTC). */
  createdAt: string;
  type: MovementType;
  /** Signed amount: negative for a charge, positive for a credit. */
  amount: number;
  /** Account balance right after this movement. */
  balanceAfter: number;
  counterpartyAlias: string;
  concept: string | null;
}
