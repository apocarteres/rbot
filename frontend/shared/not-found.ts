import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// RBOT-OPS-005, REQ-DEPLOYMENT-018
@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host { display: grid; place-items: center; min-height: 100dvh; padding: 16px; }
    .card { width: 100%; max-width: 380px; text-align: center; }
  `,
  template: `
    <section class="card">
      <h1>Страница не найдена</h1>
      <p class="muted">Проверьте адрес или вернитесь на главную.</p>
      <a routerLink="/">На главную</a>
    </section>
  `,
})
export class NotFound {}
