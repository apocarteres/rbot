import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';
import { Attempt } from '../../../../../shared/attempt';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { DaysCard } from './days-card';
import { PreviewCard } from './preview-card';
import { ScheduleApi, SessionType, Settings, Weekday } from './schedule-api';
import { SettingsCard } from './settings-card';
import { TypesCard } from './types-card';
import { WeekCard } from './week-card';

type Tab = 'week' | 'days' | 'types' | 'settings';

const TABS: readonly { readonly id: Tab; readonly title: string }[] = [
  { id: 'week', title: 'Неделя' },
  { id: 'days', title: 'Исключения' },
  { id: 'types', title: 'Типы сессий' },
  { id: 'settings', title: 'Правила записи' },
];

// MVP-02, RBOT-FEAT-003, RBOT-FEAT-004
@Component({
  selector: 'app-schedule',
  imports: [SettingsCard, WeekCard, DaysCard, TypesCard, PreviewCard, FailureDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .head { display: flex; flex-wrap: wrap; align-items: baseline; gap: 8px 12px; margin-bottom: 16px; }
    .head h1 { margin: 0; }
    .state { font-size: 0.85rem; border-radius: 6px; padding: 2px 10px; }
    .open { background: color-mix(in srgb, var(--accent) 12%, transparent); color: var(--accent); }
    .closed { background: color-mix(in srgb, var(--danger) 10%, transparent); color: var(--danger); }
    .summary { font-size: 0.85rem; color: var(--muted); }
    .link { background: none; border: 0; padding: 0; color: var(--accent); text-decoration: underline; font-size: 0.85rem; }
    .layout { display: grid; gap: 16px; grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr); align-items: start; }
    .tabs { display: flex; flex-wrap: wrap; gap: 4px; border-bottom: 1px solid var(--line); margin-bottom: 16px; }
    .tab { background: none; color: var(--muted); border: 0; border-bottom: 2px solid transparent; border-radius: 0; padding: 8px 10px; }
    .tab[aria-selected='true'] { color: var(--text); border-bottom-color: var(--text); font-weight: 600; }
    .count { font-size: 0.75rem; background: var(--bg); border-radius: 6px; padding: 0 6px; margin-left: 4px; }
    @media (max-width: 860px) { .layout { grid-template-columns: minmax(0, 1fr); } }
  `,
  template: `
    @if (settings(); as current) {
      <div class="head">
        <h1>Расписание</h1>
        @if (current.complete) {
          <span class="state open">Запись открыта</span>
          <span class="summary">{{ summary() }}</span>
        } @else {
          <span class="state closed">Запись закрыта</span>
          <button type="button" class="link" (click)="open('settings')">Заполните правила записи</button>
        }
      </div>
      <div class="layout">
        <section class="card">
          <div class="tabs" role="tablist" aria-label="Разделы расписания">
            @for (one of tabs; track one.id) {
              <button type="button" role="tab" class="tab" [id]="'tab-' + one.id" [attr.aria-selected]="tab() === one.id"
                [attr.aria-controls]="'panel-' + one.id" (click)="open(one.id)">
                {{ one.title }}
                @if (one.id === 'days' && exceptions() > 0) { <span class="count">{{ exceptions() }}</span> }
              </button>
            }
          </div>
          <div role="tabpanel" [id]="'panel-' + tab()" [attr.aria-labelledby]="'tab-' + tab()">
            @switch (tab()) {
              @case ('week') { <app-week-card [week]="week()" [types]="types()" (saved)="week.set($event); touch()" /> }
              @case ('days') { <app-days-card [zone]="current.zone" [types]="types()" (saved)="touch()" (counted)="exceptions.set($event)" /> }
              @case ('types') { <app-types-card [initial]="types()" (saved)="types.set($event)" /> }
              @case ('settings') { <app-settings-card [settings]="current" (saved)="settings.set($event)" /> }
            }
          </div>
        </section>
        <app-preview-card [settings]="current" [types]="activeTypes()" [version]="version()" />
      </div>
    }
    @if (attempt.failure()) {
      <app-failure-dialog [message]="attempt.failure()" (closed)="attempt.dismiss()" />
    }
  `,
})
export class SchedulePage implements OnInit {
  private readonly api = inject(ScheduleApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly attempt = new Attempt();
  protected readonly tabs = TABS;
  protected readonly settings = signal<Settings | null>(null);
  protected readonly week = signal<readonly Weekday[]>([]);
  protected readonly types = signal<readonly SessionType[]>([]);
  protected readonly exceptions = signal(0);
  protected readonly version = signal(0);
  protected readonly activeTypes = computed(() => this.types().filter((one) => one.active));
  protected readonly tab = toSignal(this.route.queryParamMap.pipe(map((params) => tabOf(params.get('tab')))), {
    initialValue: 'week',
  });
  protected readonly summary = computed(() => {
    const current = this.settings();
    if (!current?.complete) {
      return '';
    }
    return [current.zone, `за ${hours(current.leadMinutes ?? 0)}`, `на ${current.horizonDays} дн.`,
      `шаг ${current.slotStepMinutes} мин`, `перерыв ${current.bufferMinutes} мин`].join(' · ');
  });

  ngOnInit(): void {
    void this.attempt.run(async () => {
      const loaded = await Promise.all([this.api.settings(), this.api.week(), this.api.types()]);
      this.week.set(loaded[1]);
      this.types.set(loaded[2]);
      this.settings.set(loaded[0]);
    });
  }

  protected open(tab: Tab): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { tab }, replaceUrl: true });
  }

  protected touch(): void {
    this.version.update((value) => value + 1);
  }
}

function tabOf(value: string | null): Tab {
  return TABS.some((one) => one.id === value) ? (value as Tab) : 'week';
}

function hours(minutes: number): string {
  return minutes % 60 === 0 ? `${minutes / 60} ч` : `${minutes} мин`;
}
