import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export interface CancellationRule {
  readonly id: string;
  readonly hours: number;
  readonly allowed: boolean;
  readonly text: string;
}

export type RuleDraft = Omit<CancellationRule, 'id'>;

// RBOT-FEAT-026
@Injectable({ providedIn: 'root' })
export class RulesApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/cabinet/cancellation-rules';

  list(): Promise<readonly CancellationRule[]> {
    return firstValueFrom(this.http.get<readonly CancellationRule[]>(this.base));
  }

  add(rule: RuleDraft): Promise<CancellationRule> {
    return firstValueFrom(this.http.post<CancellationRule>(this.base, rule));
  }

  change(id: string, rule: RuleDraft): Promise<CancellationRule> {
    return firstValueFrom(this.http.put<CancellationRule>(`${this.base}/${id}`, rule));
  }

  remove(id: string): Promise<unknown> {
    return firstValueFrom(this.http.delete(`${this.base}/${id}`));
  }
}
