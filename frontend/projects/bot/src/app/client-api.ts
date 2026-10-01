import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { insideTelegram } from './telegram';

export type SessionFormat = 'IN_PERSON' | 'ONLINE';

export interface OfferedType {
  readonly id: string;
  readonly title: string;
  readonly durationMinutes: number;
  readonly price: number;
  readonly format: SessionFormat;
  readonly firstVisit: boolean;
}

export interface Offer {
  readonly open: boolean;
  readonly zone: string | null;
  readonly leadMinutes: number | null;
  readonly horizonDays: number | null;
  readonly types: readonly OfferedType[];
}

export interface Slot {
  readonly start: string;
  readonly end: string;
}

export interface ClientSession {
  readonly id: string;
  readonly typeId: string | null;
  readonly title: string | null;
  readonly format: SessionFormat | null;
  readonly start: string;
  readonly end: string;
  readonly status: string;
  readonly price: number;
}

// MVP-05, MVP-08, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009
@Injectable({ providedIn: 'root' })
export class ClientApi {
  private readonly http = inject(HttpClient);
  private readonly base = insideTelegram() ? '/api/miniapp' : '/api/client';

  offer(): Promise<Offer> {
    return firstValueFrom(this.http.get<Offer>(`${this.base}/offer`));
  }

  slots(type: string, from: string, to: string): Promise<readonly Slot[]> {
    return firstValueFrom(this.http.get<readonly Slot[]>(`${this.base}/slots`, { params: { type, from, to } }));
  }

  sessions(): Promise<readonly ClientSession[]> {
    return firstValueFrom(this.http.get<readonly ClientSession[]>(`${this.base}/sessions`));
  }

  book(typeId: string, start: string): Promise<ClientSession> {
    return firstValueFrom(this.http.post<ClientSession>(`${this.base}/sessions`, { typeId, start }));
  }

  reschedule(id: string, start: string): Promise<ClientSession> {
    return firstValueFrom(this.http.post<ClientSession>(`${this.base}/sessions/${id}/reschedule`, { start }));
  }

  cancel(id: string): Promise<ClientSession> {
    return firstValueFrom(this.http.post<ClientSession>(`${this.base}/sessions/${id}/cancel`, {}));
  }
}
