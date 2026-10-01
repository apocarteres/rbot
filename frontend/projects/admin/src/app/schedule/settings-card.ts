import { ChangeDetectionStrategy, Component, inject, input, OnInit, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Attempt } from '../../../../../shared/attempt';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { ScheduleApi, Settings } from './schedule-api';

const ZONES = ['Europe/Kaliningrad', 'Europe/Moscow', 'Europe/Samara', 'Asia/Yekaterinburg', 'Asia/Omsk', 'Asia/Novosibirsk',
  'Asia/Krasnoyarsk', 'Asia/Irkutsk', 'Asia/Yakutsk', 'Asia/Vladivostok', 'Asia/Magadan', 'Asia/Kamchatka'];

// MVP-02, RBOT-FEAT-003
@Component({
  selector: 'app-settings-card',
  imports: [FormsModule, FailureDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form (ngSubmit)="save()">
      <p class="muted">Параметры записи действуют для всех типов сессий. Пока они не заполнены, клиенты не могут записаться.</p>
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
      <button type="submit" [disabled]="attempt.busy()">Сохранить</button>
    </form>
    @if (attempt.failure()) {
      <app-failure-dialog [message]="attempt.failure()" (closed)="attempt.dismiss()" />
    }
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

  protected async save(): Promise<void> {
    await this.attempt.run(async () => {
      const saved = await this.api.saveSettings({
        zone: this.zone,
        leadMinutes: this.leadHours === null ? null : Math.round(this.leadHours * 60),
        horizonDays: this.horizonDays,
        slotStepMinutes: this.stepMinutes,
        bufferMinutes: this.bufferMinutes,
      });
      this.saved.emit(saved);
    });
  }
}
