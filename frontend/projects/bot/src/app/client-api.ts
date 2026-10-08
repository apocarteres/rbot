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
}

export interface Offer {
  readonly open: boolean;
  readonly zone: string | null;
  readonly leadMinutes: number | null;
  readonly horizonDays: number | null;
  readonly types: readonly OfferedType[];
  readonly cancellation: readonly string[];
}

export interface Cancellation {
  readonly allowed: boolean;
  readonly text: string;
}

export interface Practice {
  readonly id: string;
  readonly name: string;
}

export interface Slot {
  readonly start: string;
  readonly end: string;
}

export interface ClientSession {
  readonly id: string;
  readonly practice: string;
  readonly practiceName: string;
  readonly typeId: string | null;
  readonly title: string | null;
  readonly format: SessionFormat | null;
  readonly start: string;
  readonly end: string;
  readonly status: string;
  readonly price: number;
}

export interface Consent {
  readonly practice: string;
  readonly practiceName: string;
  readonly version: number;
  readonly text: string;
}

// MVP-05, MVP-08, RBOT-FEAT-002, RBOT-FEAT-005, RBOT-FEAT-009, RBOT-FEAT-017, RBOT-FEAT-018, RBOT-FEAT-026
@Injectable({ providedIn: 'root' })
export class ClientApi {
  private readonly http = inject(HttpClient);
  private readonly base = insideTelegram() ? '/api/miniapp' : '/api/client';

  invitation(token: string): Promise<Consent> {
    return firstValueFrom(this.http.post<Consent>(`${this.base}/invitation`, { token }));
  }

  acceptInvitation(token: string, version: number): Promise<unknown> {
    return firstValueFrom(this.http.post(`${this.base}/invitation/accept`, { token, version }));
  }

  consents(): Promise<readonly Consent[]> {
    return firstValueFrom(this.http.get<readonly Consent[]>(`${this.base}/consents`));
  }

  consent(practice: string, version: number): Promise<unknown> {
    return firstValueFrom(this.http.post(`${this.base}/consents`, { practice, version }));
  }

  practices(): Promise<readonly Practice[]> {
    return firstValueFrom(this.http.get<readonly Practice[]>(`${this.base}/practices`));
  }

  offer(practice: string): Promise<Offer> {
    return firstValueFrom(this.http.get<Offer>(`${this.base}/offer`, { params: { practice } }));
  }

  slots(practice: string, type: string, from: string, to: string): Promise<readonly Slot[]> {
    return firstValueFrom(this.http.get<readonly Slot[]>(`${this.base}/slots`, { params: { practice, type, from, to } }));
  }

  sessions(): Promise<readonly ClientSession[]> {
    return firstValueFrom(this.http.get<readonly ClientSession[]>(`${this.base}/sessions`));
  }

  book(practice: string, typeId: string, start: string): Promise<ClientSession> {
    return firstValueFrom(this.http.post<ClientSession>(`${this.base}/sessions`, { practice, typeId, start }));
  }

  reschedule(id: string, start: string): Promise<ClientSession> {
    return firstValueFrom(this.http.post<ClientSession>(`${this.base}/sessions/${id}/reschedule`, { start }));
  }

  cancellation(id: string): Promise<Cancellation> {
    return firstValueFrom(this.http.get<Cancellation>(`${this.base}/sessions/${id}/cancellation`));
  }

  cancel(id: string): Promise<ClientSession> {
    return firstValueFrom(this.http.post<ClientSession>(`${this.base}/sessions/${id}/cancel`, {}));
  }
}
