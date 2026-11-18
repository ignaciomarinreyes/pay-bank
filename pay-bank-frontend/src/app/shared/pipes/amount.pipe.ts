import { formatNumber } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

/**
 * Formats an amount in euros with Spanish separators, e.g. "1.234,56 €".
 * With `signed`, a "+" or "-" is put in front of non-zero values.
 */
@Pipe({ name: 'appAmount' })
export class AmountPipe implements PipeTransform {
  transform(value: number | null | undefined, signed = false): string {
    if (value === null || value === undefined) {
      return '';
    }
    const formatted = formatNumber(Math.abs(value), 'es', '1.2-2');
    const sign = signed && value !== 0 ? (value > 0 ? '+' : '-') : value < 0 && !signed ? '-' : '';
    return `${sign}${formatted} €`;
  }
}
