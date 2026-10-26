import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { RANK_STYLES } from '../../core/models/ranks';
import { CelebrationService } from '../../core/services/celebration.service';
import { IconComponent } from './icon.component';
import { RankBadgeComponent } from './rank-badge.component';

@Component({
  selector: 'app-celebration',
  imports: [IconComponent, RankBadgeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (reward(); as r) {
      <div class="backdrop" role="dialog" aria-modal="true" aria-labelledby="celebrate-title">
        <div class="sheet pop-in">
          <div class="burst"><app-icon name="sparkles" [size]="38" /></div>
          <h2 id="celebrate-title">Saha! Spot cleaned.</h2>
          <p class="muted">The AI confirmed it. The city says thank you.</p>

          <div class="gains">
            <div class="gain">
              <strong>+{{ r.xpGained }}</strong>
              <span>XP</span>
            </div>
            <div class="gain gold">
              <strong>+{{ r.coinsGained }}</strong>
              <span>coins</span>
            </div>
          </div>

          @if (rankedUp()) {
            <div class="rankup">
              <app-rank-badge [rank]="r.rankAfter" [size]="64" />
              <div>
                <div class="eyebrow">Rank up</div>
                <h3>Mabrouk! You're now {{ rankLabel() }}</h3>
                @if (r.giftCoins > 0) {
                  <p class="small muted">+{{ r.giftCoins }} coins level-up gift</p>
                }
              </div>
            </div>
          }

          <button type="button" class="btn btn-primary block lg" (click)="close()">Yallah, next one!</button>
        </div>
      </div>
    }
  `,
  styles: `
    .backdrop {
      position: fixed;
      inset: 0;
      z-index: 2500;
      display: grid;
      place-items: center;
      padding: 16px;
      background: rgba(11, 58, 34, 0.55);
      backdrop-filter: blur(4px);
    }
    .sheet {
      width: min(100%, 400px);
      background: #fff;
      border-radius: var(--radius-lg);
      padding: 28px 20px 20px;
      text-align: center;
      display: flex;
      flex-direction: column;
      gap: 14px;
      box-shadow: var(--shadow-lg);
    }
    .burst {
      width: 76px;
      height: 76px;
      margin: -66px auto 0;
      border-radius: 50%;
      display: grid;
      place-items: center;
      color: #fff;
      background: radial-gradient(circle at 35% 25%, var(--brand-500), var(--brand-700));
      border: 5px solid #fff;
      box-shadow: var(--shadow);
    }
    h2 {
      font-size: 26px;
      color: var(--brand-900);
    }
    .gains {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
    }
    .gain {
      padding: 14px;
      border-radius: var(--radius);
      background: var(--brand-50);
      border: 1px solid var(--brand-100);
      strong {
        display: block;
        font-size: 30px;
        color: var(--brand-700);
      }
      span {
        font-weight: 800;
        color: var(--ink-2);
      }
    }
    .gain.gold {
      background: var(--gold-100);
      border-color: #f6e2a8;
      strong {
        color: #a67400;
      }
    }
    .rankup {
      display: flex;
      align-items: center;
      gap: 14px;
      text-align: left;
      padding: 14px;
      border-radius: var(--radius);
      background: linear-gradient(135deg, #fff8e6, #fff);
      border: 1.5px solid #f6e2a8;
      h3 {
        font-size: 17px;
      }
    }
  `,
})
export class CelebrationOverlayComponent {
  private readonly celebration = inject(CelebrationService);
  protected readonly reward = this.celebration.current;
  protected readonly rankedUp = computed(() => {
    const r = this.reward();
    return !!r && r.rankAfter !== r.rankBefore;
  });
  protected readonly rankLabel = computed(() => {
    const r = this.reward();
    return r ? RANK_STYLES[r.rankAfter].label : '';
  });

  protected close(): void {
    this.celebration.close();
  }
}
