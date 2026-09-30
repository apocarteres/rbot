import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AppUpdate } from '@apocarteres/app-update';

// REQ-CLIENT-UPDATE-001, REQ-CLIENT-UPDATE-003
@Component({
  selector: 'app-update-available',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .banner {
      position: fixed; inset: auto 16px 16px; z-index: 10; display: flex; flex-wrap: wrap; align-items: center; gap: 12px;
      max-width: 560px; margin: 0 auto; padding: 12px 16px;
      background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius);
      box-shadow: 0 6px 24px rgb(0 0 0 / 0.12);
    }
    .banner span { flex: 1; min-width: 180px; }
  `,
  template: `
    <div class="banner" role="status">
      <span>Вышла новая версия. Обновите страницу, когда закончите текущее действие.</span>
      <button type="button" (click)="update.reload()">Обновить</button>
    </div>
  `,
})
export class UpdateAvailable {
  protected readonly update = inject(AppUpdate);
}
