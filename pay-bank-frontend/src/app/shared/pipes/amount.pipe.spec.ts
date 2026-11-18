import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { AmountPipe } from './amount.pipe';

describe('AmountPipe', () => {
  const pipe = new AmountPipe();

  beforeAll(() => registerLocaleData(localeEs));

  it('formats signed amounts', () => {
    expect(pipe.transform(-1234.56, true)).toBe('-1.234,56 €');
    expect(pipe.transform(50, true)).toBe('+50,00 €');
    expect(pipe.transform(0, true)).toBe('0,00 €');
  });

  it('formats unsigned amounts', () => {
    expect(pipe.transform(150)).toBe('150,00 €');
    expect(pipe.transform(120)).toBe('120,00 €');
    expect(pipe.transform(1234567.8)).toBe('1.234.567,80 €');
  });

  it('keeps the minus of a negative balance when unsigned', () => {
    expect(pipe.transform(-234.5)).toBe('-234,50 €');
  });

  it('returns an empty string without a value', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });
});
