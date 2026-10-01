import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from './dialog-focus';

// RBOT-FEAT-003, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-failure-dialog',
  imports: [ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <section class="dialog" role="alertdialog" aria-modal="true" aria-labelledby="failure-title" aria-describedby="failure-text"
        apcrModal [apcrModalEscape]="close">
        <h2 id="failure-title">Не получилось</h2>
        <p id="failure-text">{{ message() }}</p>
        <div class="dialog-actions">
          <button type="button" apcrLocal (click)="close()">Понятно</button>
        </div>
      </section>
    </div>
  `,
})
export class FailureDialog {
  readonly message = input.required<string>();
  readonly closed = output<void>();

  protected readonly close = (): void => this.closed.emit();

  constructor() {
    focusFirstField();
  }
}
