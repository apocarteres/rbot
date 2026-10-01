import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { SessionFormat } from '../schedule/schedule-api';

export type SessionStatus = 'REQUESTED' | 'BOOKED' | 'DECLINED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW';

export interface CabinetSession {
  readonly id: string;
  readonly typeId: string | null;
  readonly title: string | null;
  readonly format: SessionFormat | null;
  readonly start: string;
  readonly end: string;
  readonly status: SessionStatus;
  readonly price: number;
  readonly clientName: string | null;
  readonly cancelledByClient: boolean;
  readonly rescheduled: boolean;
}

// MVP-05, RBOT-FEAT-005, RBOT-FEAT-009
@Injectable({ providedIn: 'root' })
export class SessionsApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/cabinet';

  between(from: string, to: string): Promise<readonly CabinetSession[]> {
    return firstValueFrom(this.http.get<readonly CabinetSession[]>(`${this.base}/sessions`, { params: { from, to } }));
  }

  book(clientId: string, typeId: string, start: string): Promise<CabinetSession> {
    return firstValueFrom(this.http.post<CabinetSession>(`${this.base}/sessions`, { clientId, typeId, start }));
  }

  reschedule(id: string, start: string): Promise<CabinetSession> {
    return firstValueFrom(this.http.post<CabinetSession>(`${this.base}/sessions/${id}/reschedule`, { start }));
  }

  cancel(id: string): Promise<CabinetSession> {
    return firstValueFrom(this.http.post<CabinetSession>(`${this.base}/sessions/${id}/cancel`, {}));
  }

  noShow(id: string): Promise<CabinetSession> {
    return firstValueFrom(this.http.post<CabinetSession>(`${this.base}/sessions/${id}/no-show`, {}));
  }
}
