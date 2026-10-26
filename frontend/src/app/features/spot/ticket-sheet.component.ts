import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, output } from '@angular/core';

import { IconComponent } from '../../shared/components/icon.component';
import { QrCodeComponent } from '../../shared/components/qr-code.component';
import { TICKET_PREFIX } from './qr-scanner.component';

/** Personal QR ticket shown to the organizer on site. Keeps the screen awake while open. */
@Component({
  selector: 'app-ticket-sheet',
  imports: [QrCodeComponent, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="backdrop" (click)="closed.emit()" role="presentation"></div>
    <section class="sheet pop-in" role="dialog" aria-modal="true" aria-labelledby="ticket-title">
      <header>
        <h2 id="ticket-title">Your ticket</h2>
        <button type="button" class="icon-btn" (click)="closed.emit()" aria-label="Close"><app-icon name="x" /></button>
      </header>
      <p class="muted small">Show this to the organizer on site to check in.</p>
      <div class="ticket">
        <app-qr-code [value]="prefix + code()" [size]="280" />
        <div class="code">{{ code() }}</div>
        <div class="name">{{ name() }}</div>
      </div>
    </section>
  `,
  styles: `
    .backdrop {
      position: fixed;
      inset: 0;
      z-index: 2000;
      background: rgba(11, 58, 34, 0.5);
    }
    .sheet {
      position: fixed;
      z-index: 2001;
      left: 0;
      right: 0;
      margin-inline: auto;
      bottom: 0;
      width: min(100%, var(--col));
      background: #fff;
      border-radius: var(--radius-lg) var(--radius-lg) 0 0;
      padding: 20px 20px calc(24px + var(--safe-bottom));
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .ticket {
      margin: 6px auto 0;
      width: min(100%, 300px);
      padding: 16px;
      border-radius: var(--radius);
      border: 2px dashed var(--brand-200);
      text-align: center;
      background: var(--brand-50);
    }
    .code {
      margin-top: 10px;
      font-size: 26px;
      font-weight: 900;
      letter-spacing: 0.25em;
      color: var(--brand-900);
    }
    .name {
      font-weight: 800;
      color: var(--ink-2);
    }
  `,
})
export class TicketSheetComponent {
  readonly code = input.required<string>();
  readonly name = input('');
  readonly closed = output<void>();
  protected readonly prefix = TICKET_PREFIX;

  constructor() {
    let lock: WakeLockSentinel | null = null;
    navigator.wakeLock
      ?.request('screen')
      .then((l) => (lock = l))
      .catch(() => undefined);
    inject(DestroyRef).onDestroy(() => void lock?.release());
  }
}
