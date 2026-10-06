import { inject, Injectable, signal } from '@angular/core';
import type { Notice } from '@apocarteres/notifications';
import { clock, dayTitle, isoDate } from '../../../../../shared/dates';
import { ClientsApi } from '../clients/clients-api';
import { ScheduleApi } from '../schedule/schedule-api';

// RBOT-FEAT-020, REQ-NOTIFICATIONS-003
@Injectable({ providedIn: 'root' })
export class NoticeTexts {
  private readonly clients = inject(ClientsApi);
  private readonly schedule = inject(ScheduleApi);
  private readonly names = signal<ReadonlyMap<string, string>>(new Map());
  private readonly zone = signal('Europe/Moscow');

  async load(): Promise<void> {
    const [clients, settings] = await Promise.all([this.clients.list(), this.schedule.settings()]);
    this.names.set(new Map(clients.map((one) => [one.id, one.name])));
    this.zone.set(settings.zone);
  }

  text(notice: Notice): string {
    const who = this.names().get(notice.params['client'] ?? '') ?? 'Клиент';
    const type = notice.params['type'] ? ` — ${notice.params['type']}` : '';
    const when = this.when(notice.params['start']);
    switch (notice.kind) {
      case 'session.booked':
        return `Новая запись: ${who}${type}, ${when}`;
      case 'session.rescheduled':
        return `Перенос: ${who}${type}, ${this.when(notice.params['previous'])} → ${when}`;
      case 'session.cancelled':
        return `Отмена: ${who}${type}, ${when}`;
      default:
        return 'Уведомление';
    }
  }

  private when(instant: string | undefined): string {
    if (!instant) {
      return '';
    }
    return `${dayTitle(isoDate(Date.parse(instant), this.zone()))}, ${clock(instant, this.zone())}`;
  }
}
