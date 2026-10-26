import { ChangeDetectionStrategy, Component } from '@angular/core';

import { LogoComponent } from './logo.component';

/** Animated splash shown while the session is restored. */
@Component({
  selector: 'app-splash',
  imports: [LogoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="splash" aria-label="Loading NQIYA">
      <div class="blob"></div>
      <app-logo [size]="72" [withText]="false" class="grow" />
      <h1>NQIYA</h1>
      <p>Spot a mess. Clean it together. <b>Level up.</b></p>
    </div>
  `,
  styles: `
    .splash {
      position: fixed;
      inset: 0;
      z-index: 4000;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 14px;
      background: var(--bg);
      overflow: hidden;
      animation: fade-out 0.35s ease 1.1s forwards;
    }
    .blob {
      position: absolute;
      width: 520px;
      height: 520px;
      right: -220px;
      top: -140px;
      border-radius: 46% 54% 60% 40%;
      background: var(--brand-100);
    }
    .grow {
      animation: grow 0.8s cubic-bezier(0.2, 1.4, 0.4, 1) both;
      z-index: 1;
    }
    h1 {
      z-index: 1;
      font-size: 44px;
      letter-spacing: 0.14em;
      color: var(--brand-900);
      animation: rise 0.6s 0.2s both;
    }
    p {
      z-index: 1;
      font-weight: 800;
      color: var(--ink-2);
      animation: rise 0.6s 0.35s both;
      b {
        color: var(--brand-600);
      }
    }
    @keyframes grow {
      from {
        transform: scale(0.3) rotate(-30deg);
        opacity: 0;
      }
    }
    @keyframes rise {
      from {
        transform: translateY(14px);
        opacity: 0;
      }
    }
    @keyframes fade-out {
      to {
        opacity: 0;
        visibility: hidden;
      }
    }
  `,
})
export class SplashComponent {}
