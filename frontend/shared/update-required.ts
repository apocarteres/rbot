import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AppUpdate } from '@apocarteres/app-update';
import { ApcrModal, ApcrModalBackdrop, BACKDROP_IGNORED, ESCAPE_IGNORED } from '@apocarteres/modal';

// REQ-CLIENT-UPDATE-001, REQ-CLIENT-UPDATE-003, RBOT-ARC-002, RBOT-ARC-003, RBOT-ARC-004, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-002, REQ-CLIENT-MODAL-007, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-update-required',
  imports: [ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .veil { position: fixed; inset: 0; z-index: 20; display: grid; place-items: center; padding: 16px; background: rgb(0 0 0 / 0.4); }
    .card { max-width: 420px; }
  `,
  template: `
    <div class="veil" [apcrModalBackdrop]="ignoreBackdrop">
      <div class="card" role="alertdialog" aria-modal="true" aria-labelledby="update-required-title" apcrModal [apcrModalEscape]="ignoreEscape">
        <h2 id="update-required-title">Страница устарела</h2>
        <p>Эта версия больше не работает с сервером. Обновите страницу, чтобы продолжить.</p>
        <button type="button" apcrLocal (click)="update.reload()">Обновить страницу</button>
      </div>
    </div>
  `,
})
export class UpdateRequired {
  protected readonly update = inject(AppUpdate);
  protected readonly ignoreEscape = ESCAPE_IGNORED;
  protected readonly ignoreBackdrop = BACKDROP_IGNORED;
}
