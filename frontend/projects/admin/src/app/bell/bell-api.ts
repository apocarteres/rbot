import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

interface BellView {
  readonly sound: boolean;
}

// RBOT-FEAT-020
@Injectable({ providedIn: 'root' })
export class BellApi {
  private readonly http = inject(HttpClient);

  async sound(): Promise<boolean> {
    return (await firstValueFrom(this.http.get<BellView>('/api/cabinet/bell'))).sound;
  }

  async changeSound(sound: boolean): Promise<boolean> {
    return (await firstValueFrom(this.http.put<BellView>('/api/cabinet/bell', { sound }))).sound;
  }
}
