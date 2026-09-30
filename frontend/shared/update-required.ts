import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AppUpdate } from '@apocarteres/app-update';

// REQ-CLIENT-UPDATE-001, REQ-CLIENT-UPDATE-003
@Component({
  selector: 'app-update-required',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .veil { position: fixed; inset: 0; z-index: 20; display: grid; place-items: center; padding: 16px; background: rgb(0 0 0 / 0.4); }
    .card { max-width: 420px; }
  `,
  template: `
    <div class="veil">
      <div class="card" role="alertdialog" aria-live="assertive" aria-labelledby="update-required-title">
        <h2 id="update-required-title">Страница устарела</h2>
        <p>Эта версия больше не работает с сервером. Обновите страницу, чтобы продолжить.</p>
        <button type="button" (click)="update.reload()">Обновить страницу</button>
      </div>
    </div>
  `,
})
export class UpdateRequired {
  protected readonly update = inject(AppUpdate);
}
