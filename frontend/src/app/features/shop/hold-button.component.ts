import { ChangeDetectionStrategy, Component, input, OnDestroy, output, signal } from '@angular/core';

/**
 * Press-and-hold to confirm (used to redeem a voucher at the counter so it can't be burned by a stray tap).
 * Keyboard users can hold Space / Enter.
 */
@Component({
  selector: 'app-hold-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      type="button"
      class="hold"
      [disabled]="disabled()"
      (pointerdown)="begin($event)"
      (pointerup)="cancel()"
      (pointerleave)="cancel()"
      (pointercancel)="cancel()"
      (keydown.space)="begin($event)"
      (keydown.enter)="begin($event)"
      (keyup.space)="cancel()"
      (keyup.enter)="cancel()"
      (contextmenu)="$event.preventDefault()"
    >
      <span class="fill" [style.transform]="'scaleX(' + progress() + ')'"></span>
      <span class="label">{{ progress() > 0 ? 'Keep holding…' : label() }}</span>
    </button>
  `,
  styles: `
    .hold {
      position: relative;
      width: 100%;
      min-height: 56px;
      border-radius: 999px;
      border: 0;
      overflow: hidden;
      background: var(--brand-900);
      color: #fff;
      font-weight: 900;
      font-size: 16px;
      cursor: pointer;
      user-select: none;
      -webkit-user-select: none;
      touch-action: none;
    }
    .hold:disabled {
      opacity: 0.5;
    }
    .fill {
      position: absolute;
      inset: 0;
      transform-origin: left;
      background: linear-gradient(90deg, var(--brand-500), var(--gold));
    }
    .label {
      position: relative;
    }
  `,
})
export class HoldButtonComponent implements OnDestroy {
  readonly label = input('Hold to use');
  readonly durationMs = input(1500);
  readonly disabled = input(false);
  readonly confirmed = output<void>();

  protected readonly progress = signal(0);
  private startedAt = 0;
  private raf = 0;

  protected begin(event: Event): void {
    event.preventDefault();
    if (this.raf) return;
    this.startedAt = performance.now();
    const step = (now: number) => {
      const p = Math.min(1, (now - this.startedAt) / this.durationMs());
      this.progress.set(p);
      if (p >= 1) {
        this.raf = 0;
        navigator.vibrate?.(80);
        this.confirmed.emit();
        this.progress.set(0);
        return;
      }
      this.raf = requestAnimationFrame(step);
    };
    this.raf = requestAnimationFrame(step);
  }

  protected cancel(): void {
    cancelAnimationFrame(this.raf);
    this.raf = 0;
    this.progress.set(0);
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.raf);
  }
}
