import { ChangeDetectionStrategy, Component, inject, input, OnInit, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Attempt } from './attempt';
import { ScheduleApi, Settings } from './schedule-api';

const ZONES = ['Europe/Kaliningrad', 'Europe/Moscow', 'Europe/Samara', 'Asia/Yekaterinburg', 'Asia/Omsk', 'Asia/Novosibirsk',
  'Asia/Krasnoyarsk', 'Asia/Irkutsk', 'Asia/Yakutsk', 'Asia/Vladivostok', 'Asia/Magadan', 'Asia/Kamchatka'];

// MVP-02
@Component({
  selector: 'app-settings-card',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="card" (ngSubmit)="save()">
      <h2>Параметры записи</h2>
      @if (!settings().complete) {
        <p class="warning">Пока параметры не заполнены, запись клиентов закрыта.</p>
      }
      <div class="grid">
        <div class="field">
          <label for="zone">Часовой пояс</label>
          <select id="zone" name="zone" [(ngModel)]="zone">
            @for (one of zones; track one) {
              <option [value]="one">{{ one }}</option>
            }
          </select>
        </div>
        <div class="field">
          <label for="lead">Минимум до сессии, ч</label>
          <input id="lead" name="lead" type="number" min="0" max="168" [(ngModel)]="leadHours" />
        </div>
        <div class="field">
          <label for="horizon">Запись вперёд, дней</label>
          <input id="horizon" name="horizon" type="number" min="1" max="365" [(ngModel)]="horizonDays" />
        </div>
        <div class="field">
          <label for="step">Шаг слотов, мин</label>
          <input id="step" name="step" type="number" min="5" max="240" step="5" [(ngModel)]="stepMinutes" />
        </div>
        <div class="field">
          <label for="buffer">Перерыв после сессии, мин</label>
          <input id="buffer" name="buffer" type="number" min="0" max="240" step="5" [(ngModel)]="bufferMinutes" />
        </div>
      </div>
      @if (attempt.error()) {
        <p class="error" role="alert">{{ attempt.error() }}</p>
      }
      @if (attempt.notice()) {
        <p class="notice" role="status">{{ attempt.notice() }}</p>
      }
      <button type="submit" [disabled]="attempt.busy()">Сохранить</button>
    </form>
  `,
})
export class SettingsCard implements OnInit {
  readonly settings = input.required<Settings>();
  readonly saved = output<Settings>();

  private readonly api = inject(ScheduleApi);
  protected readonly attempt = new Attempt();
  protected readonly zones = ZONES;

  protected zone = 'Europe/Moscow';
  protected leadHours: number | null = null;
  protected horizonDays: number | null = null;
  protected stepMinutes: number | null = null;
  protected bufferMinutes: number | null = null;

  ngOnInit(): void {
    const settings = this.settings();
    this.zone = settings.zone;
    this.leadHours = settings.leadMinutes === null ? null : settings.leadMinutes / 60;
    this.horizonDays = settings.horizonDays;
    this.stepMinutes = settings.slotStepMinutes;
    this.bufferMinutes = settings.bufferMinutes;
  }

  protected save(): Promise<void> {
    return this.attempt.run(async () => {
      const saved = await this.api.saveSettings({
        zone: this.zone,
        leadMinutes: this.leadHours === null ? null : Math.round(this.leadHours * 60),
        horizonDays: this.horizonDays,
        slotStepMinutes: this.stepMinutes,
        bufferMinutes: this.bufferMinutes,
      });
      this.saved.emit(saved);
      return 'Параметры сохранены.';
    });
  }
}
