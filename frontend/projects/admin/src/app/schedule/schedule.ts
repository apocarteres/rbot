import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { DaysCard } from './days-card';
import { PreviewCard } from './preview-card';
import { ScheduleApi, SessionType, Settings, Weekday } from './schedule-api';
import { SettingsCard } from './settings-card';
import { TypesCard } from './types-card';
import { WeekCard } from './week-card';
import { Attempt } from './attempt';

// MVP-02
@Component({
  selector: 'app-schedule',
  imports: [SettingsCard, WeekCard, DaysCard, TypesCard, PreviewCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .stack { display: flex; flex-direction: column; gap: 24px; margin-top: 24px; }
  `,
  template: `
    <h1>Расписание</h1>
    <p class="muted">Рабочие часы, исключения и типы сессий. Клиенты записываются только на свободное время внутри расписания.</p>
    @if (attempt.error()) {
      <p class="error" role="alert">{{ attempt.error() }}</p>
    }
    @if (settings(); as current) {
      <div class="stack">
        <app-settings-card [settings]="current" (saved)="settings.set($event)" />
        <app-week-card [week]="week()" (saved)="touch()" />
        <app-days-card [zone]="current.zone" (saved)="touch()" />
        <app-types-card [initial]="types()" (saved)="types.set($event)" />
        <app-preview-card [settings]="current" [types]="activeTypes()" [version]="version()" />
      </div>
    }
  `,
})
export class SchedulePage implements OnInit {
  private readonly api = inject(ScheduleApi);
  protected readonly attempt = new Attempt();
  protected readonly settings = signal<Settings | null>(null);
  protected readonly week = signal<readonly Weekday[]>([]);
  protected readonly types = signal<readonly SessionType[]>([]);
  protected readonly version = signal(0);
  protected readonly activeTypes = computed(() => this.types().filter((one) => one.active));

  ngOnInit(): void {
    void this.attempt.run(async () => {
      const [settings, week, types] = await Promise.all([this.api.settings(), this.api.week(), this.api.types()]);
      this.week.set(week);
      this.types.set(types);
      this.settings.set(settings);
    });
  }

  protected touch(): void {
    this.version.update((value) => value + 1);
  }
}
