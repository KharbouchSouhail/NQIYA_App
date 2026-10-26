import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ToastService } from '../../core/services/toast.service';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-toasts',
  imports: [IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="stack" aria-live="polite" role="status">
      @for (t of toasts.toasts(); track t.id) {
        <button type="button" class="toast pop-in" [class]="t.kind" (click)="toasts.dismiss(t.id)">
          <app-icon [name]="t.kind === 'success' ? 'check' : t.kind === 'error' ? 'alert' : 'sparkles'" [size]="20" />
          <span>{{ t.message }}</span>
        </button>
      }
    </div>
  `,
  styles: `
    .stack {
      position: fixed;
      top: calc(12px + var(--safe-top));
      left: 50%;
      transform: translateX(-50%);
      width: min(calc(100% - 24px), 440px);
      z-index: 3000;
      display: flex;
      flex-direction: column;
      gap: 8px;
      pointer-events: none;
    }
    .toast {
      pointer-events: auto;
      display: flex;
      align-items: center;
      gap: 10px;
      width: 100%;
      text-align: left;
      padding: 12px 16px;
      border-radius: 16px;
      border: 0;
      font-weight: 800;
      font-size: 14px;
      background: var(--brand-900);
      color: #fff;
      box-shadow: var(--shadow-lg);
      cursor: pointer;
    }
    .toast.success app-icon {
      color: #7fd18f;
    }
    .toast.error {
      background: #fff;
      color: #8f1d20;
      border: 1.5px solid #f3c9ca;
    }
    .toast.info app-icon {
      color: var(--cleaning);
    }
  `,
})
export class ToastContainerComponent {
  protected readonly toasts = inject(ToastService);
}
