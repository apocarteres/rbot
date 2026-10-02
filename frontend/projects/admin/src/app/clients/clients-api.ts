import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type ClientChannel = 'TELEGRAM' | 'EMAIL' | 'INVITED';

export interface ClientView {
  readonly id: string;
  readonly name: string;
  readonly email: string | null;
  readonly channel: ClientChannel;
  readonly inviteExpiresAt: string | null;
}

export interface InviteView {
  readonly clientId: string;
  readonly link: string | null;
  readonly expiresAt: string;
}

export interface ConsentView {
  readonly version: number | null;
  readonly body: string;
  readonly savedAt: string | null;
}

// MVP-03, RBOT-FEAT-009, RBOT-FEAT-018
@Injectable({ providedIn: 'root' })
export class ClientsApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/cabinet/clients';

  list(): Promise<readonly ClientView[]> {
    return firstValueFrom(this.http.get<readonly ClientView[]>(this.base));
  }

  invite(label: string): Promise<InviteView> {
    return firstValueFrom(this.http.post<InviteView>(this.base, { label }));
  }

  reinvite(id: string): Promise<InviteView> {
    return firstValueFrom(this.http.post<InviteView>(`${this.base}/${id}/invite`, {}));
  }

  consent(): Promise<ConsentView> {
    return firstValueFrom(this.http.get<ConsentView>('/api/cabinet/consent'));
  }

  saveConsent(body: string): Promise<ConsentView> {
    return firstValueFrom(this.http.put<ConsentView>('/api/cabinet/consent', { body }));
  }
}
