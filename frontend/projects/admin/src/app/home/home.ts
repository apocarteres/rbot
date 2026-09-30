import { ChangeDetectionStrategy, Component } from '@angular/core';

// MVP-01
@Component({
  selector: 'app-home',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .grid { display: grid; gap: 16px; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); margin-top: 24px; }
    .soon { font-size: 0.8rem; color: var(--muted); text-transform: uppercase; letter-spacing: 0.04em; }
  `,
  template: `
    <h1>Добро пожаловать</h1>
    <p class="muted">Кабинет работает. Разделы ниже появятся по плану MVP.</p>
    <div class="grid">
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
    { title: 'Расписание', text: 'Рабочие дни и часы, отпуск, типы сессий.' },
    { title: 'Клиенты', text: 'Карточки клиентов и приглашения в бот.' },
    { title: 'Записи', text: 'Календарь сессий, переносы и отмены.' },
    { title: 'Правила отмены', text: 'Политика отмен и штрафы.' },
  ];
}
