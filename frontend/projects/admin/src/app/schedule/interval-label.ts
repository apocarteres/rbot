import { time } from '../../../../../shared/dates';
import { Interval, SessionType } from './schedule-api';

// MVP-02, RBOT-FEAT-004, REQ-CODE-DESIGN-007
export function intervalLabel(interval: Interval, types: readonly SessionType[]): string {
  const hours = `${time(interval.start)}–${time(interval.end)}`;
  if (interval.types.length === 0) {
    return hours;
  }
  const titles = interval.types.map((id) => types.find((one) => one.id === id)?.title ?? '?');
  return `${hours} · ${titles.join(', ')}`;
}
