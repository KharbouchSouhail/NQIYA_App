import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import jsQR from 'jsqr';

import { IconComponent } from '../../shared/components/icon.component';

export const TICKET_PREFIX = 'NQIYA:';

/** Camera QR scanner with a manual-entry fallback. Emits the raw ticket code. */
@Component({
  selector: 'app-qr-scanner',
  imports: [FormsModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="viewport">
      @if (cameraError()) {
        <div class="no-cam">
          <app-icon name="camera" [size]="36" [stroke]="1.6" />
          <p class="small">{{ cameraError() }}</p>
        </div>
      } @else {
        <video #video playsinline muted></video>
        <span class="frame"></span>
      }
    </div>
    <form class="manual" (ngSubmit)="submitManual()">
      <input
        class="input"
        name="code"
        [(ngModel)]="manualCode"
        placeholder="Or type the ticket code"
        autocapitalize="characters"
        maxlength="12"
        aria-label="Ticket code"
      />
      <button class="btn btn-dark" type="submit" [disabled]="!manualCode.trim()">Check in</button>
    </form>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .viewport {
      position: relative;
      aspect-ratio: 1;
      border-radius: var(--radius);
      overflow: hidden;
      background: #0b1a12;
      display: grid;
      place-items: center;
    }
    video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .frame {
      position: absolute;
      inset: 18%;
      border-radius: 18px;
      border: 3px solid rgba(255, 255, 255, 0.9);
      box-shadow: 0 0 0 999px rgba(0, 0, 0, 0.35);
    }
    .no-cam {
      color: #cfe7d6;
      text-align: center;
      padding: 24px;
    }
    .manual {
      display: flex;
      gap: 8px;
      .input {
        text-transform: uppercase;
      }
    }
  `,
})
export class QrScannerComponent {
  readonly scanned = output<string>();

  private readonly video = viewChild<ElementRef<HTMLVideoElement>>('video');
  protected readonly cameraError = signal<string | null>(null);
  protected manualCode = '';

  private stream?: MediaStream;
  private rafId = 0;
  private pausedUntil = 0;
  private readonly canvas = document.createElement('canvas');

  constructor() {
    afterNextRender(() => void this.start());
    inject(DestroyRef).onDestroy(() => this.stop());
  }

  protected submitManual(): void {
    const code = this.manualCode.trim().toUpperCase();
    if (code) {
      this.scanned.emit(code);
      this.manualCode = '';
    }
  }

  private async start(): Promise<void> {
    if (!navigator.mediaDevices?.getUserMedia) {
      this.cameraError.set('Camera not available — type the code instead.');
      return;
    }
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' }, audio: false });
      const el = this.video()?.nativeElement;
      if (!el) return;
      el.srcObject = this.stream;
      await el.play();
      this.tick();
    } catch {
      this.cameraError.set('Camera permission denied — type the code instead.');
    }
  }

  private tick = (): void => {
    const el = this.video()?.nativeElement;
    if (el && el.readyState === el.HAVE_ENOUGH_DATA && Date.now() > this.pausedUntil) {
      const w = el.videoWidth;
      const h = el.videoHeight;
      this.canvas.width = w;
      this.canvas.height = h;
      const ctx = this.canvas.getContext('2d', { willReadFrequently: true });
      if (ctx) {
        ctx.drawImage(el, 0, 0, w, h);
        const result = jsQR(ctx.getImageData(0, 0, w, h).data, w, h, { inversionAttempts: 'dontInvert' });
        if (result?.data) {
          const raw = result.data.trim();
          const code = raw.startsWith(TICKET_PREFIX) ? raw.slice(TICKET_PREFIX.length) : raw;
          this.pausedUntil = Date.now() + 2000; // avoid double check-ins of the same ticket
          navigator.vibrate?.(60);
          this.scanned.emit(code.toUpperCase());
        }
      }
    }
    this.rafId = requestAnimationFrame(this.tick);
  };

  private stop(): void {
    cancelAnimationFrame(this.rafId);
    this.stream?.getTracks().forEach((t) => t.stop());
  }
}
