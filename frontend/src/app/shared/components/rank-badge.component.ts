import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { Rank } from '../../core/models/models';
import { RANK_STYLES } from '../../core/models/ranks';
import { IconComponent } from './icon.component';

/** Rank insignia: a shield tinted with the rank colour; Diamond gets an animated legendary glow. */
@Component({
  selector: 'app-rank-badge',
  imports: [IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="badge"
      [class.legend]="rank() === 'DIAMOND'"
      [style.--rank]="style().color"
      [style.--glow]="style().glow"
      [style.width.px]="size()"
      [style.height.px]="size()"
    >
      <app-icon name="shield" [size]="size() * 0.62" [stroke]="2.2" />
    </span>
    @if (showLabel()) {
      <span class="label" [style.color]="style().color">{{ style().label }}</span>
    }
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
    .badge {
      display: grid;
      place-items: center;
      border-radius: 30%;
      color: #fff;
      background: linear-gradient(145deg, color-mix(in srgb, var(--rank) 70%, #fff), var(--rank));
      box-shadow: 0 4px 12px var(--glow);
    }
    .legend {
      animation: legend 2.4s ease-in-out infinite;
    }
    .label {
      font-weight: 900;
      font-size: 13px;
    }
    @keyframes legend {
      50% {
        box-shadow:
          0 0 0 4px rgba(224, 165, 38, 0.35),
          0 6px 22px var(--glow);
      }
    }
  `,
})
export class RankBadgeComponent {
  readonly rank = input.required<Rank>();
  readonly size = input(28);
  readonly showLabel = input(false);
  protected readonly style = computed(() => RANK_STYLES[this.rank()]);
}
