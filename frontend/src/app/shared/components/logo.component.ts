import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { IconComponent } from './icon.component';

@Component({
  selector: 'app-logo',
  imports: [IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="mark" [style.width.px]="size()" [style.height.px]="size()">
      <app-icon name="sprout" [size]="size() * 0.6" [stroke]="2.4" />
    </span>
    @if (withText()) {
      <span class="word" [style.font-size.px]="size() * 0.62">NQIYA</span>
      <span class="flag" [style.height.px]="size() * 0.5" role="img" aria-label="Morocco">
        <svg viewBox="0 0 30 20"><rect width="30" height="20" fill="#c1272d" /><path d="M15 6.2l1.1 3.4h3.6l-2.9 2.1 1.1 3.4-2.9-2.1-2.9 2.1 1.1-3.4-2.9-2.1h3.6z" fill="none" stroke="#006233" stroke-width=".9" /></svg>
      </span>
    }
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }
    .mark {
      display: grid;
      place-items: center;
      border-radius: 50%;
      color: #fff;
      background: radial-gradient(circle at 30% 25%, var(--brand-500), var(--brand-700));
      box-shadow: 0 4px 12px rgba(31, 138, 70, 0.35);
    }
    .word {
      font-weight: 900;
      letter-spacing: 0.12em;
      color: var(--brand-900);
    }
    .flag svg {
      height: 100%;
      width: auto;
      border-radius: 2px;
      display: block;
    }
  `,
})
export class LogoComponent {
  readonly size = input(34);
  readonly withText = input(true);
}
