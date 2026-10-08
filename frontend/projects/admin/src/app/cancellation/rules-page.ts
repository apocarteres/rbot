import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Attempt } from '../../../../../shared/attempt';
import { FailureDialog } from '../../../../../shared/failure-dialog';
import { ConfirmDialog } from '../sessions/confirm-dialog';
import { RuleDialog } from './rule-dialog';
import { CancellationRule, RulesApi } from './rules-api';

type Editing = { readonly rule: CancellationRule | null } | null;

// RBOT-FEAT-026, ADR-0004
@Component({
  selector: 'app-rules-page',
  imports: [RuleDialog, ConfirmDialog, FailureDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .head { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; margin-bottom: 8px; }
    .head h1 { margin: 0; margin-right: auto; }
    .rule { display: flex; flex-wrap: wrap; gap: 8px 12px; align-items: flex-start; padding: 10px 0; border-top: 1px solid var(--line); }
    .rule:first-child { border-top: 0; }
    .hours { font-weight: 600; min-width: 140px; }
    .about { display: flex; flex-direction: column; gap: 4px; flex: 1; min-width: 200px; }
    .text { overflow-wrap: anywhere; white-space: pre-line; color: var(--muted); }
    .state { align-self: flex-start; font-size: 0.8rem; border-radius: 6px; padding: 2px 8px; white-space: nowrap; }
    .on { background: color-mix(in srgb, var(--accent) 12%, transparent); color: var(--accent); }
    .off { background: color-mix(in srgb, var(--danger) 10%, transparent); color: var(--danger); }
    .icon { display: inline-grid; place-items: center; padding: 8px; line-height: 0; }
  `,
  template: `
    <div class="head">
      <h1>Правила отмены</h1>
      <button type="button" (click)="editing.set({ rule: null })">Добавить правило</button>
    </div>
    <p class="muted">Клиент видит правила при записи. Когда клиент отменяет или переносит запись, срабатывает правило с наибольшим порогом, не больше времени до начала. Если ни одно не сработало, клиент видит «Для отмены обратитесь к психологу». Вы отменяете и переносите записи без ограничений.</p>
    <section class="card">
      @for (rule of rules(); track rule.id) {
        <div class="rule">
          <span class="hours">За {{ rule.hours }} ч и раньше</span>
          <span class="about">
            <span class="state" [class.on]="rule.allowed" [class.off]="!rule.allowed">{{ rule.allowed ? 'Можно отменить' : 'Нельзя отменить' }}</span>
            <span class="text">{{ rule.text }}</span>
          </span>
          <button type="button" class="quiet" [attr.aria-label]="'Изменить правило за ' + rule.hours + ' ч'" (click)="editing.set({ rule })">Изменить</button>
          <button type="button" class="quiet icon" [attr.aria-label]="'Удалить правило за ' + rule.hours + ' ч'" title="Удалить правило" (click)="removing.set(rule)">
            <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.8"
              stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M10 11v6M14 11v6M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2l1-12M9 7V4h6v3" /></svg>
          </button>
        </div>
      } @empty {
        @if (loaded()) { <p class="muted">Правил нет: на «Отменить» и «Перенести» клиент видит «Для отмены обратитесь к психологу».</p> }
      }
    </section>
    @if (editing(); as open) {
      <app-rule-dialog [rule]="open.rule" (closed)="editing.set(null)" (saved)="applied($event)" />
    }
    @if (removing(); as doomed) {
      <app-confirm-dialog heading="Удалить правило?" [text]="'Правило «за ' + doomed.hours + ' ч» перестанет действовать для всех записей.'"
        confirm="Удалить" [action]="remove(doomed)" (closed)="removing.set(null)" (done)="removed(doomed)" />
    }
    @if (attempt.failure()) {
      <app-failure-dialog [message]="attempt.failure()" (closed)="attempt.dismiss()" />
    }
  `,
})
export class RulesPage implements OnInit {
  private readonly api = inject(RulesApi);
  protected readonly attempt = new Attempt();
  protected readonly rules = signal<readonly CancellationRule[]>([]);
  protected readonly loaded = signal(false);
  protected readonly editing = signal<Editing>(null);
  protected readonly removing = signal<CancellationRule | null>(null);

  ngOnInit(): void {
    void this.load();
  }

  protected remove(rule: CancellationRule): () => Promise<unknown> {
    return () => this.api.remove(rule.id);
  }

  protected async removed(rule: CancellationRule): Promise<void> {
    this.removing.set(null);
    this.rules.update((rules) => rules.filter((one) => one.id !== rule.id));
    await this.load();
  }

  protected async applied(rule: CancellationRule): Promise<void> {
    this.editing.set(null);
    this.rules.update((rules) => sorted([...rules.filter((one) => one.id !== rule.id), rule]));
    await this.load();
  }

  private async load(): Promise<void> {
    await this.attempt.run(async () => {
      this.rules.set(await this.api.list());
      this.loaded.set(true);
    });
  }
}

function sorted(rules: readonly CancellationRule[]): readonly CancellationRule[] {
  return [...rules].sort((a, b) => b.hours - a.hours);
}
