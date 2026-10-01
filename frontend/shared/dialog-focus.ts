import { afterNextRender, ElementRef, inject, Injector } from '@angular/core';

// RBOT-ARC-002, REQ-CLIENT-MODAL-003
export function focusFirstField(): void {
  const host = inject<ElementRef<HTMLElement>>(ElementRef);
  afterNextRender(() => host.nativeElement.querySelector<HTMLElement>('input, select, textarea, button')?.focus(), {
    injector: inject(Injector),
  });
}
