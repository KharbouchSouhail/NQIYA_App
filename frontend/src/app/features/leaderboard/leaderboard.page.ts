import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { LeaderboardEntry } from '../../core/models/models';
import { RANK_STYLES } from '../../core/models/ranks';
import { LeaderboardService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { IconComponent } from '../../shared/components/icon.component';
import { RankBadgeComponent } from '../../shared/components/rank-badge.component';

@Component({
  selector: 'app-leaderboard-page',
  imports: [DecimalPipe, IconComponent, RankBadgeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <header class="page-header">
        <h1>Leaderboard</h1>
        <span class="spacer"></span>
        <button type="button" class="icon-btn" (click)="board.reload()" aria-label="Refresh">
          <app-icon name="refresh" />
        </button>
      </header>

      @if (board.isLoading() && !board.hasValue()) {
        <div class="skeleton" style="height: 220px"></div>
        <div class="skeleton" style="height: 300px"></div>
      } @else if (board.error()) {
        <div class="empty card">
          <div class="big">😕</div>
          <p>Couldn't load the leaderboard.</p>
          <button class="btn btn-outline sm" (click)="board.reload()">Try again</button>
        </div>
      } @else if (board.value(); as b) {
        @if (b.top.length === 0) {
          <div class="empty card">
            <div class="big">🏆</div>
            <p>No cleaners yet. Be the first legend!</p>
          </div>
        } @else {
          <section class="podium" aria-label="Top 3">
            @for (p of podium(); track p.userId) {
              <div class="step" [class]="'p' + p.position">
                <app-rank-badge [rank]="p.rank" [size]="p.position === 1 ? 58 : 46" />
                <strong class="name" [class.legend]="p.rank === 'DIAMOND'">{{ p.displayName }}</strong>
                <span class="xp">{{ p.xp | number }} XP</span>
                <div class="block">
                  <span>{{ p.position }}</span>
                </div>
              </div>
            }
          </section>

          @if (b.me; as me) {
            <div class="card me">
              <span class="pos">#{{ me.position }}</span>
              <app-rank-badge [rank]="me.rank" [size]="34" />
              <div class="grow">
                <strong>You</strong>
                <span class="small muted">{{ rankLabel(me) }}</span>
              </div>
              <strong>{{ me.xp | number }} XP</strong>
            </div>
          }

          <ol class="card ranking">
            @for (e of rest(); track e.userId) {
              <li [class.mine]="e.userId === auth.me()?.id">
                <span class="pos">{{ e.position }}</span>
                <app-rank-badge [rank]="e.rank" [size]="30" />
                <span class="grow name" [class.legend]="e.rank === 'DIAMOND'">{{ e.displayName }}</span>
                <span class="xp">{{ e.xp | number }} XP</span>
              </li>
            }
          </ol>
        }
      }
    </div>
  `,
  styles: `
    .podium {
      display: grid;
      grid-template-columns: 1fr 1.15fr 1fr;
      align-items: end;
      gap: 8px;
      padding-top: 8px;
    }
    .step {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      text-align: center;
      min-width: 0;
    }
    .p1 {
      order: 2;
    }
    .p2 {
      order: 1;
    }
    .p3 {
      order: 3;
    }
    .name {
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .legend {
      background: linear-gradient(90deg, var(--brand-600), var(--gold));
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }
    .xp {
      font-size: 12px;
      font-weight: 800;
      color: var(--ink-2);
    }
    .block {
      width: 100%;
      margin-top: 6px;
      border-radius: 16px 16px 6px 6px;
      display: grid;
      place-items: center;
      color: #fff;
      font-size: 26px;
      font-weight: 900;
      background: linear-gradient(180deg, var(--brand-500), var(--brand-700));
      box-shadow: var(--shadow);
    }
    .p1 .block {
      height: 120px;
      background: linear-gradient(180deg, #f1c24c, var(--gold));
    }
    .p2 .block {
      height: 90px;
    }
    .p3 .block {
      height: 70px;
      background: linear-gradient(180deg, #c98b55, #a8652f);
    }
    .me {
      display: flex;
      align-items: center;
      gap: 12px;
      border-color: var(--brand-200);
      background: var(--brand-50);
      .grow {
        flex: 1;
        display: flex;
        flex-direction: column;
      }
    }
    .ranking {
      list-style: none;
      margin: 0;
      padding: 6px 12px;
      li {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px 0;
        border-bottom: 1px solid var(--line);
      }
      li:last-child {
        border-bottom: 0;
      }
      li.mine {
        font-weight: 900;
      }
      .grow {
        flex: 1;
        min-width: 0;
      }
    }
    .pos {
      width: 28px;
      text-align: center;
      font-weight: 900;
      color: var(--ink-3);
    }
  `,
})
export class LeaderboardPage {
  private readonly api = inject(LeaderboardService);
  protected readonly auth = inject(AuthService);

  protected readonly board = rxResource({ stream: () => this.api.get(50) });

  protected readonly podium = computed(() => this.board.value()?.top.slice(0, 3) ?? []);
  protected readonly rest = computed(() => this.board.value()?.top.slice(3) ?? []);

  protected rankLabel(e: LeaderboardEntry): string {
    return RANK_STYLES[e.rank].label;
  }
}
