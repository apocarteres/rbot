import { clock, dayTitle, isoDate } from '../../../../shared/dates';
import { SessionFormat } from './client-api';

const RUBLES = new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 2, minimumFractionDigits: 0 });

export const DEFAULT_ZONE = 'Europe/Moscow';

const FORMATS: Readonly<Record<SessionFormat, string>> = { IN_PERSON: 'очно', ONLINE: 'онлайн' };

// MVP-08, RBOT-FEAT-002, REQ-CODE-DESIGN-007
export function rubles(price: number): string {
  return RUBLES.format(price);
}

// MVP-08, RBOT-FEAT-002
export function details(title: string | null, format: SessionFormat | null, price: number): string {
  const shown = format && !(title ?? '').toLowerCase().includes(FORMATS[format]) ? [FORMATS[format]] : [];
  return [...shown, rubles(price)].join(' · ');
}

// MVP-08, RBOT-FEAT-002
export function shortDay(date: string): string {
  const [year, month, day] = date.split('-').map(Number);
  return new Intl.DateTimeFormat('ru-RU', { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' })
    .format(Date.UTC(year, month - 1, day));
}

// MVP-08, RBOT-FEAT-002
export function when(start: string, end: string, zone: string): string {
  return `${dayTitle(isoDate(Date.parse(start), zone))}, ${clock(start, zone)}–${clock(end, zone)}`;
}

// MVP-08, RBOT-FEAT-002, ADR-0003
export function zoneNote(zone: string): string {
  return zone === DEFAULT_ZONE ? 'Время московское' : `Время по поясу ${zone}`;
}
