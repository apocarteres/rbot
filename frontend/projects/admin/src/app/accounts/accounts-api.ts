import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface AccountView {
  readonly id: string;
  readonly email: string;
  readonly roles: readonly string[];
  readonly blocked: boolean;
  readonly createdAt: string;
  readonly lastLoginAt: string | null;
}

export interface AccountPage {
  readonly items: readonly AccountView[];
  readonly total: number;
  readonly page: number;
  readonly size: number;
}

// MVP-01, REQ-AUTH-009
@Injectable({ providedIn: 'root' })
export class AccountsApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/admin/accounts';

  list(query: string, page: number): Promise<AccountPage> {
    return firstValueFrom(this.http.get<AccountPage>(this.base, { params: { query, page } }));
  }

  create(email: string, password: string, roles: readonly string[]): Promise<AccountView> {
    return firstValueFrom(this.http.post<AccountView>(this.base, { email, password, roles }));
  }

  setPassword(id: string, password: string): Promise<void> {
    return firstValueFrom(this.http.put<void>(`${this.base}/${id}/password`, { password }));
  }

  block(id: string): Promise<void> {
    return firstValueFrom(this.http.post<void>(`${this.base}/${id}/block`, {}));
  }

  unblock(id: string): Promise<void> {
    return firstValueFrom(this.http.post<void>(`${this.base}/${id}/unblock`, {}));
  }
}
