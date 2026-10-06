import { ChangeDetectionStrategy, Component, effect, ElementRef, HostListener, inject, OnInit, signal, untracked } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { NotificationBell } from '@apocarteres/notifications';
import type { Notice } from '@apocarteres/notifications';
import { BellApi } from './bell-api';
import { BellChime } from './bell-chime';
import { NoticeTexts } from './notice-texts';

// RBOT-FEAT-020, REQ-NOTIFICATIONS-005
@Component({
  selector: 'app-notification-bell',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host { position: relative; display: inline-flex; }
    .bell-button { position: relative; display: inline-flex; align-items: center; padding: 4px 8px; line-height: 1; }
    .bell-button svg { width: 20px; height: 20px; }
    .badge { position: absolute; top: -4px; right: -4px; min-width: 18px; padding: 1px 5px; border-radius: 999px; background: var(--danger);
      color: #fff; font-size: 0.7rem; font-weight: 600; line-height: 16px; text-align: center; }
    .panel { position: absolute; top: calc(100% + 8px); right: 0; z-index: 20; width: min(360px, calc(100vw - 32px)); padding: 12px;
      background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); box-shadow: 0 6px 24px rgb(0 0 0 / 0.12); }
    .head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 8px; }
    .list { list-style: none; margin: 0; padding: 0; max-height: 320px; overflow: auto; }
    .item { display: block; width: 100%; text-align: left; padding: 8px; border: 0; border-radius: 6px; background: none; color: var(--text);
      font: inherit; font-size: 0.9rem; line-height: 1.35; cursor: pointer; }
    .item:hover { background: color-mix(in srgb, var(--accent) 8%, transparent); }
    .empty { margin: 8px 0; font-size: 0.9rem; }
    .foot { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px; margin-top: 8px; padding-top: 8px;
      border-top: 1px solid var(--line); font-size: 0.85rem; }
    .sound { display: inline-flex; align-items: center; gap: 12px; }
    .sound label { display: inline-flex; align-items: center; gap: 6px; margin: 0; }
    .link { background: none; border: 0; padding: 0; color: var(--accent); font-size: 0.85rem; cursor: pointer; }
  `,
  template: `
    <button type="button" class="quiet bell-button" [attr.aria-expanded]="open()" [attr.aria-label]="label()" [title]="label()" (click)="toggle()">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9" /><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
      </svg>
      @if (bell.unread() > 0) {
        <span class="badge">{{ bell.unread() > 99 ? '99+' : bell.unread() }}</span>
      }
    </button>
    @if (open()) {
      <div class="panel" role="region" aria-label="Уведомления">
        <div class="head">
          <strong>Уведомления</strong>
          @if (bell.unread() > 0) {
            <button type="button" class="link" (click)="readAll()">Прочитать все</button>
          }
        </div>
        @if (bell.items(); as items) {
          @if (items.length > 0) {
            <ul class="list">
              @for (one of items; track one.id) {
                <li><button type="button" class="item" (click)="follow(one)">{{ texts.text(one) }}</button></li>
              }
            </ul>
          } @else {
            <p class="muted empty">Новых уведомлений нет.</p>
          }
        } @else {
          <p class="muted empty">Загрузка…</p>
        }
        <div class="foot">
          <a routerLink="/notifications" (click)="open.set(false)">Все уведомления</a>
          <span class="sound">
            <label><input type="checkbox" [checked]="sound()" (change)="changeSound($any($event.target).checked)" /> Звук</label>
            <button type="button" class="link" (click)="chime.preview()">Проверить звук</button>
          </span>
        </div>
      </div>
    }
  `,
})
export class NotificationBellComponent implements OnInit {
  protected readonly bell = inject(NotificationBell);
  protected readonly texts = inject(NoticeTexts);
  protected readonly chime = inject(BellChime);
  private readonly api = inject(BellApi);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly open = signal(false);
  protected readonly sound = signal(false);
  private heard: number | null = null;

  constructor() {
    effect(() => {
      const unread = this.bell.unread();
      untracked(() => this.hear(unread));
    });
  }

  ngOnInit(): void {
    void this.bell.refresh().then(() => (this.heard = this.bell.unread()), () => undefined);
    void this.api.sound().then((on) => this.sound.set(on), () => undefined);
  }

  protected label(): string {
    const unread = this.bell.unread();
    return unread > 0 ? `Уведомления: непрочитанных ${unread}` : 'Уведомления';
  }

  protected async toggle(): Promise<void> {
    if (this.open()) {
      this.open.set(false);
      return;
    }
    this.open.set(true);
    await Promise.all([this.texts.load().catch(() => undefined), this.bell.open().catch(() => undefined)]);
  }

  protected async follow(notice: Notice): Promise<void> {
    this.open.set(false);
    await this.bell.read(notice.id).catch(() => undefined);
    if (notice.link) {
      await this.router.navigateByUrl(notice.link);
    }
  }

  protected readAll(): void {
    void this.bell.readAll().catch(() => undefined);
  }

  protected changeSound(on: boolean): void {
    this.sound.set(on);
    if (on) {
      this.chime.prime();
    }
    void this.api.changeSound(on).then((saved) => this.sound.set(saved), () => this.sound.set(!on));
  }

  @HostListener('document:click', ['$event'])
  protected outside(event: Event): void {
    if (this.sound()) {
      this.chime.prime();
    }
    if (this.open() && event.target instanceof Node && !this.host.nativeElement.contains(event.target)) {
      this.open.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  protected escape(): void {
    this.open.set(false);
  }

  private hear(unread: number): void {
    if (this.heard === null) {
      return;
    }
    if (unread > this.heard && this.sound()) {
      this.chime.play();
    }
    this.heard = unread;
  }
}
