import { ChangeDetectionStrategy, Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';
import { ClientsApi, ConsentView } from './clients-api';

// MVP-03, RBOT-FEAT-018, ADR-0005, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-consent-dialog',
  imports: [FormsModule, ApcrAction, ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .dialog { width: min(720px, 100%); }
    textarea { width: 100%; min-height: 320px; resize: vertical; font: inherit; line-height: 1.45; }
    .small { font-size: 0.85rem; }
  `,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="consent-dialog-title" apcrModal [apcrModalEscape]="close"
        (submit)="$event.preventDefault()">
        <h2 id="consent-dialog-title">Текст согласия</h2>
        <div class="field">
          <label for="consent-body">Что клиент прочтёт перед кнопкой «Согласен»</label>
          <textarea id="consent-body" name="body" required maxlength="10000" aria-describedby="consent-body-hint" [(ngModel)]="body"></textarea>
          <span id="consent-body-hint" class="muted small">Клиенты, которые уже дали согласие, увидят новый текст при следующем входе в приложение и смогут записываться к вам только после того, как примут его.</span>
        </div>
        @if (failure()) {
          <p class="error" role="alert">{{ failure() }}</p>
        }
        <div class="dialog-actions">
          <button type="button" class="quiet" apcrLocal (click)="close()">Отмена</button>
          <button type="submit" [apcrAction]="save" [apcrActionFailure]="failed" (apcrActionDone)="saved.emit($any($event))">Сохранить</button>
        </div>
      </form>
    </div>
  `,
})
export class ConsentDialog implements OnInit {
  readonly consent = input.required<ConsentView>();
  readonly closed = output<void>();
  readonly saved = output<ConsentView>();

  private readonly api = inject(ClientsApi);
  protected body = '';
  protected readonly failure = signal('');
  protected readonly close = (): void => this.closed.emit();
  protected readonly failed = (failure: unknown): void => this.failure.set(failureMessage(failure));
  protected readonly save = (): Promise<ConsentView> => {
    this.failure.set('');
    return this.api.saveConsent(this.body);
  };

  constructor() {
    focusFirstField();
  }

  ngOnInit(): void {
    this.body = this.consent().body;
  }
}
