import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// MVP-01, MVP-02, MVP-14, RBOT-FEAT-005, RBOT-FEAT-009
@Component({
  selector: 'app-home',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .grid { display: grid; gap: 16px; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); margin-top: 24px; }
    .ready { text-decoration: none; color: inherit; border-color: var(--accent); }
    .soon { font-size: 0.8rem; color: var(--muted); text-transform: uppercase; letter-spacing: 0.04em; }
  `,
  template: `
    <h1>Добро пожаловать</h1>
    <p class="muted">Остальные разделы появятся по плану MVP.</p>
    <div class="grid">
      <a class="card ready" routerLink="/sessions">
        <h2>Записи</h2>
        <p class="muted">Записи клиентов по неделям, запись клиента, перенос, отмена и неявка.</p>
      </a>
      <a class="card ready" routerLink="/clients">
        <h2>Клиенты</h2>
        <p class="muted">Приглашения в бот Telegram и список клиентов.</p>
      </a>
      <a class="card ready" routerLink="/schedule">
        <h2>Расписание</h2>
        <p class="muted">Рабочие дни и часы, отпуск, типы сессий, предпросмотр записи.</p>
      </a>
      @for (section of sections; track section.title) {
        <section class="card">
          <span class="soon">Скоро</span>
          <h2>{{ section.title }}</h2>
          <p class="muted">{{ section.text }}</p>
        </section>
      }
    </div>
  `,
})
export class Home {
  protected readonly sections = [
    { title: 'Анкеты', text: 'Анкеты новых клиентов.' },
    { title: 'Правила отмены', text: 'Политика отмен и штрафы.' },
  ];
}
