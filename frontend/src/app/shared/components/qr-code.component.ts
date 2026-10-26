import { ChangeDetectionStrategy, Component, effect, ElementRef, input, viewChild } from '@angular/core';
import QRCode from 'qrcode';

@Component({
  selector: 'app-qr-code',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<canvas #canvas [attr.aria-label]="'QR code ' + value()" role="img"></canvas>`,
  styles: `
    canvas {
      display: block;
      width: 100%;
      height: auto;
      image-rendering: pixelated;
    }
  `,
})
export class QrCodeComponent {
  readonly value = input.required<string>();
  readonly size = input(260);
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');

  constructor() {
    effect(() => {
      void QRCode.toCanvas(this.canvas().nativeElement, this.value(), {
        width: this.size(),
        margin: 1,
        errorCorrectionLevel: 'M',
        color: { dark: '#0b3a22', light: '#ffffff' },
      });
    });
  }
}
