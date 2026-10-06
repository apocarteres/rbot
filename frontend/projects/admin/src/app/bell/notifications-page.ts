import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationBell } from '@apocarteres/notifications';
import type { Notice } from '@apocarteres/notifications';
import { Attempt } from '../../../../../shared/attempt';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { NoticeTexts } from './notice-texts';

const SIZE = 20;
const CREATED = new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });

// RBOT-FEAT-020, REQ-NOTIFICATIONS-009
@Component({
  selector: 'app-notifications-page',
  imports: [FailureDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .head { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; margin-bottom: 16px; }
    .head h1 { margin: 0; margin-right: auto; }
    .row { display: flex; flex-wrap: wrap; align-items: baseline; gap: 4px 12px; width: 100%; padding: 10px 0; border: 0;
      border-top: 1px solid var(--line); background: none; color: var(--text); font: inherit; text-align: left; cursor: pointer; }
    .row:first-child { border-top: 0; }
    .text { flex: 1 1 260px; }
    .unread .text { font-weight: 600; }
    .when { font-size: 0.85rem; }
    .pages { display: flex; align-items: center; justify-content: flex-end; gap: 12px; margin-top: 12px; font-size: 0.9rem; }
  `,
  template: `
    <div class="head">
      <h1>Уведомления</h1>
      @if (bell.unread() > 0) {
        <button type="button" class="quiet" (click)="readAll()">Прочитать все</button>
      }
    </div>
    <section class="card">
      @for (one of items(); track one.id) {
        <button type="button" class="row" [class.unread]="!one.read" (click)="follow(one)">
          <span class="text">{{ texts.text(one) }}</span>
          <span class="muted when">{{ created(one) }}</span>
        </button>
      } @empty {
        @if (loaded()) { <p class="muted">Уведомлений нет. Здесь появятся записи, переносы и отмены, которые клиенты делают сами.</p> }
      }
      @if (pages() > 1) {
        <div class="pages">
          <button type="button" class="quiet" [disabled]="page() === 0" (click)="go(page() - 1)">‹ Новее</button>
          <span class="muted">{{ page() + 1 }} из {{ pages() }}</span>
          <button type="button" class="quiet" [disabled]="page() + 1 >= pages()" (click)="go(page() + 1)">Старше ›</button>
        </div>
      }
    </section>
    @if (attempt.failure()) {
      <app-failure-dialog [message]="attempt.failure()" (closed)="attempt.dismiss()" />
    }
  `,
})
export class NotificationsPage implements OnInit {
  protected readonly bell = inject(NotificationBell);
  protected readonly texts = inject(NoticeTexts);
  private readonly router = inject(Router);
  protected readonly attempt = new Attempt();
  protected readonly items = signal<readonly Notice[]>([]);
  protected readonly page = signal(0);
  protected readonly pages = signal(0);
  protected readonly loaded = signal(false);

  ngOnInit(): void {
    void this.attempt.run(async () => {
      await this.texts.load();
      await this.load(0);
    });
  }

  protected created(notice: Notice): string {
    return CREATED.format(Date.parse(notice.createdAt));
  }

  protected go(page: number): void {
    void this.attempt.run(() => this.load(page));
  }

  protected readAll(): void {
    void this.attempt.run(async () => {
      await this.bell.readAll();
      await this.load(this.page());
    });
  }

  protected follow(notice: Notice): void {
    void this.attempt.run(async () => {
      if (!notice.read) {
        await this.bell.read(notice.id);
      }
      if (notice.link) {
        await this.router.navigateByUrl(notice.link);
      } else {
        await this.load(this.page());
      }
    });
  }

  private async load(page: number): Promise<void> {
    const result = await this.bell.page(page, SIZE);
    this.items.set(result.items);
    this.page.set(result.page);
    this.pages.set(Math.ceil(result.total / SIZE));
    this.loaded.set(true);
  }
}
