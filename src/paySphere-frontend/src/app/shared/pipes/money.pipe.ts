import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'money', standalone: true })
export class MoneyPipe implements PipeTransform {
  transform(value: number | null | undefined, currencyCode?: string | null): string {
    if (value === null || value === undefined) {
      return '—';
    }
    const formatted = new Intl.NumberFormat('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);
    return currencyCode ? `${currencyCode} ${formatted}` : formatted;
  }
}
