import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AccountService } from '../../../core/services/account.service';
import { PageTitleComponent } from '../../../shared/components/page-title/page-title';
import { AmountPipe } from '../../../shared/pipes/amount.pipe';
import { PaymentErrorCode } from '../payments.models';
import { PaymentsService } from '../payments.service';

const CONCEPT_MAX_LENGTH = 140;
const AMOUNT_PATTERN = /^\d+([.,]\d{1,2})?$/;

const ERROR_MESSAGES: Record<PaymentErrorCode, string> = {
  RECIPIENT_NOT_FOUND: 'No existe ningún usuario con ese alias',
  SELF_PAYMENT: 'No puedes hacerte un pago a ti mismo',
  INSUFFICIENT_BALANCE: 'Saldo insuficiente',
};
const GENERIC_ERROR = 'No se ha podido realizar el pago. Inténtalo de nuevo más tarde.';

/** A non-empty amount must have at most 2 decimals (comma or dot) and be greater than 0. */
function amountValidator(control: AbstractControl<string>): ValidationErrors | null {
  const value = control.value.trim();
  if (value === '') {
    return null;
  }
  return AMOUNT_PATTERN.test(value) && parseAmount(value) > 0 ? null : { amount: true };
}

function parseAmount(value: string): number {
  return Number(value.trim().replace(',', '.'));
}

@Component({
  selector: 'app-new-payment-page',
  imports: [ReactiveFormsModule, PageTitleComponent, AmountPipe],
  templateUrl: './new-payment-page.html',
  styleUrl: './new-payment-page.scss',
})
export class NewPaymentPageComponent {
  private readonly accountService = inject(AccountService);
  private readonly paymentsService = inject(PaymentsService);
  private readonly router = inject(Router);

  /** Available balance; null while loading or if it could not be loaded. */
  protected readonly balance = toSignal(
    this.accountService.getMyAccount().pipe(
      map((account) => account.balance),
      catchError(() => of(null)),
    ),
    { initialValue: null },
  );

  protected readonly form = new FormGroup({
    recipientAlias: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl('', { nonNullable: true, validators: [Validators.required, amountValidator] }),
    // No maxlength attribute in the input: the user must be able to see the error
    concept: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(CONCEPT_MAX_LENGTH)] }),
  });
  protected readonly conceptMaxLength = CONCEPT_MAX_LENGTH;
  protected readonly submitting = signal(false);
  protected readonly serverError = signal<string | null>(null);

  protected submit(): void {
    if (this.submitting()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.submitting.set(true);
    this.serverError.set(null);
    this.paymentsService
      .createPayment({
        recipientAlias: value.recipientAlias.trim(),
        amount: parseAmount(value.amount),
        concept: value.concept.trim() === '' ? null : value.concept,
      })
      .subscribe({
        next: () => void this.router.navigateByUrl('/movements'),
        error: (error: unknown) => {
          this.submitting.set(false);
          this.serverError.set(this.messageFor(error));
        },
      });
  }

  private messageFor(error: unknown): string {
    if (error instanceof HttpErrorResponse && error.status === 422) {
      const code = (error.error as { code?: string } | null)?.code;
      if (code && code in ERROR_MESSAGES) {
        return ERROR_MESSAGES[code as PaymentErrorCode];
      }
    }
    return GENERIC_ERROR;
  }
}
