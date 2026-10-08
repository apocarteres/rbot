import { ChangeDetectionStrategy, Component, computed, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApcrAction } from '@apocarteres/action';
import { ApcrModal, ApcrModalBackdrop } from '@apocarteres/modal';
import { focusFirstField } from '../../../../../shared/dialog-focus';
import { failureMessage } from '../../../../../shared/failures';
import { CancellationRule, RuleDraft, RulesApi } from './rules-api';

type Editable = { -readonly [K in keyof RuleDraft]: RuleDraft[K] };

// RBOT-FEAT-026, REQ-CLIENT-MODAL-001, REQ-CLIENT-MODAL-005, REQ-CLIENT-MODAL-009
@Component({
  selector: 'app-rule-dialog',
  imports: [FormsModule, ApcrAction, ApcrModal, ApcrModalBackdrop],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .choice { display: flex; flex-wrap: wrap; gap: 8px 16px; margin-bottom: 12px; border: 0; padding: 0; }
    .choice label { display: flex; gap: 6px; align-items: center; }
    textarea { width: 100%; box-sizing: border-box; resize: vertical; font: inherit; line-height: 1.45; }
  `,
  template: `
    <div class="veil" [apcrModalBackdrop]="close">
      <form class="dialog" role="dialog" aria-modal="true" aria-labelledby="rule-dialog-title" apcrModal [apcrModalEscape]="close"
        (submit)="$event.preventDefault()">
        <h2 id="rule-dialog-title">{{ heading() }}</h2>
        <div class="field"><label for="rule-hours">Не позднее чем за, часов</label>
          <input id="rule-hours" name="hours" type="number" min="0" max="8760" step="1" required [(ngModel)]="draft.hours" /></div>
        <fieldset class="choice" aria-label="Отмена">
          <label><input type="radio" name="allowed" [value]="true" [(ngModel)]="draft.allowed" /> Можно отменить</label>
          <label><input type="radio" name="allowed" [value]="false" [(ngModel)]="draft.allowed" /> Нельзя отменить</label>
        </fieldset>
        <div class="field"><label for="rule-text">Текст для клиента</label>
          <textarea id="rule-text" name="text" rows="3" maxlength="500" required [(ngModel)]="draft.text"></textarea></div>
        @if (failure()) {
          <p class="error" role="alert">{{ failure() }}</p>
        }
        <div class="dialog-actions">
          <button type="button" class="quiet" apcrLocal (click)="close()">Отмена</button>
          <button type="submit" [apcrAction]="save" [apcrActionFailure]="failed" (apcrActionDone)="saved.emit($any($event))">{{ confirm() }}</button>
        </div>
      </form>
    </div>
  `,
})
export class RuleDialog implements OnInit {
  readonly rule = input<CancellationRule | null>(null);
  readonly closed = output<void>();
  readonly saved = output<CancellationRule>();

  private readonly api = inject(RulesApi);
  protected draft: Editable = { hours: 24, allowed: true, text: '' };
  protected readonly failure = signal('');
  protected readonly heading = computed(() => (this.rule() ? 'Правило отмены' : 'Новое правило отмены'));
  protected readonly confirm = computed(() => (this.rule() ? 'Сохранить' : 'Добавить'));
  protected readonly close = (): void => this.closed.emit();
  protected readonly failed = (failure: unknown): void => this.failure.set(failureMessage(failure));
  protected readonly save = (): Promise<CancellationRule> => {
    this.failure.set('');
    const rule = this.rule();
    const draft: RuleDraft = { hours: Number(this.draft.hours), allowed: this.draft.allowed, text: this.draft.text };
    return rule ? this.api.change(rule.id, draft) : this.api.add(draft);
  };

  constructor() {
    focusFirstField();
  }

  ngOnInit(): void {
    const rule = this.rule();
    if (rule) {
      this.draft = { hours: rule.hours, allowed: rule.allowed, text: rule.text };
    }
  }
}
