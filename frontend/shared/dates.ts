// MVP-02, RBOT-FEAT-002, REQ-CODE-DESIGN-007
export const WEEKDAYS = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье'];

export function isoDate(instant: number, zone: string): string {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit' })
    .formatToParts(instant);
  const part = (type: string): string => parts.find((one) => one.type === type)?.value ?? '';
  return `${part('year')}-${part('month')}-${part('day')}`;
}

export function plusDays(date: string, days: number): string {
  const [year, month, day] = date.split('-').map(Number);
  const shifted = new Date(Date.UTC(year, month - 1, day + days));
  return shifted.toISOString().slice(0, 10);
}

export function dayTitle(date: string): string {
  const [year, month, day] = date.split('-').map(Number);
  return new Intl.DateTimeFormat('ru-RU', { weekday: 'short', day: 'numeric', month: 'long', timeZone: 'UTC' })
    .format(Date.UTC(year, month - 1, day));
}

export function time(value: string): string {
  return value.slice(0, 5);
}

// MVP-05, RBOT-FEAT-005
export function instantAt(date: string, time: string, zone: string): string {
  const [year, month, day] = date.split('-').map(Number);
  const [hour, minute] = time.split(':').map(Number);
  const wanted = Date.UTC(year, month - 1, day, hour, minute);
  let guess = wanted;
  for (let pass = 0; pass < 2; pass++) {
    guess = wanted - (asUtc(guess, zone) - guess);
  }
  return new Date(guess).toISOString();
}

// MVP-05, RBOT-FEAT-005
export function mondayOf(date: string): string {
  const [year, month, day] = date.split('-').map(Number);
  const weekday = (new Date(Date.UTC(year, month - 1, day)).getUTCDay() + 6) % 7;
  return plusDays(date, -weekday);
}

function asUtc(instant: number, zone: string): number {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: zone, hourCycle: 'h23', year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit' }).formatToParts(instant);
  const part = (type: string): number => Number(parts.find((one) => one.type === type)?.value ?? 0);
  return Date.UTC(part('year'), part('month') - 1, part('day'), part('hour'), part('minute'), part('second'));
}

export function clock(instant: string, zone: string): string {
  return new Intl.DateTimeFormat('ru-RU', { hour: '2-digit', minute: '2-digit', timeZone: zone }).format(Date.parse(instant));
}
