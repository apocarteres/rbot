import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export type SessionFormat = 'IN_PERSON' | 'ONLINE';

export interface Interval {
  readonly start: string;
  readonly end: string;
}

export interface Settings {
  readonly zone: string;
  readonly leadMinutes: number | null;
  readonly horizonDays: number | null;
  readonly slotStepMinutes: number | null;
  readonly bufferMinutes: number | null;
  readonly complete: boolean;
}

export interface Weekday {
  readonly weekday: number;
  readonly intervals: readonly Interval[];
}

export interface Day {
  readonly date: string;
  readonly closed: boolean;
  readonly note: string | null;
  readonly intervals: readonly Interval[];
}

export interface SessionType {
  readonly id: string;
  readonly title: string;
  readonly durationMinutes: number;
  readonly price: number;
  readonly format: SessionFormat;
  readonly firstVisit: boolean;
  readonly active: boolean;
}

export interface Slot {
  readonly start: string;
  readonly end: string;
}

// MVP-02
@Injectable({ providedIn: 'root' })
export class ScheduleApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/cabinet/schedule';

  settings(): Promise<Settings> {
    return firstValueFrom(this.http.get<Settings>(`${this.base}/settings`));
  }

  saveSettings(settings: Omit<Settings, 'complete'>): Promise<Settings> {
    return firstValueFrom(this.http.put<Settings>(`${this.base}/settings`, settings));
  }

  week(): Promise<readonly Weekday[]> {
    return firstValueFrom(this.http.get<readonly Weekday[]>(`${this.base}/week`));
  }

  saveWeekday(weekday: number, intervals: readonly Interval[]): Promise<readonly Interval[]> {
    return firstValueFrom(this.http.put<readonly Interval[]>(`${this.base}/week/${weekday}`, { intervals }));
  }

  days(from: string, to: string): Promise<readonly Day[]> {
    return firstValueFrom(this.http.get<readonly Day[]>(`${this.base}/days`, { params: { from, to } }));
  }

  saveDay(date: string, closed: boolean, note: string, intervals: readonly Interval[]): Promise<Day> {
    return firstValueFrom(this.http.put<Day>(`${this.base}/days/${date}`, { closed, note, intervals }));
  }

  closeDays(from: string, to: string, note: string): Promise<{ readonly days: number }> {
    return firstValueFrom(this.http.post<{ readonly days: number }>(`${this.base}/days/closed`, { from, to, note }));
  }

  clearDay(date: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.base}/days/${date}`));
  }

  types(): Promise<readonly SessionType[]> {
    return firstValueFrom(this.http.get<readonly SessionType[]>(`${this.base}/types`));
  }

  createType(type: Omit<SessionType, 'id'>): Promise<SessionType> {
    return firstValueFrom(this.http.post<SessionType>(`${this.base}/types`, type));
  }

  saveType(type: SessionType): Promise<SessionType> {
    const { id, ...body } = type;
    return firstValueFrom(this.http.put<SessionType>(`${this.base}/types/${id}`, body));
  }

  slots(type: string, from: string, to: string): Promise<readonly Slot[]> {
    return firstValueFrom(this.http.get<readonly Slot[]>(`${this.base}/slots`, { params: { type, from, to } }));
  }
}
