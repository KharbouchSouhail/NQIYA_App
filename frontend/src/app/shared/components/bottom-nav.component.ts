import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { IconComponent, IconName } from './icon.component';

interface NavItem {
  path: string;
  label: string;
  icon: IconName;
}

/** Floating "liquid glass" navbar with the Report action in the middle. */
@Component({
  selector: 'app-bottom-nav',
  imports: [RouterLink, RouterLinkActive, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav aria-label="Main">
      @for (item of left; track item.path) {
        <a [routerLink]="item.path" routerLinkActive="active" class="tab">
          <app-icon [name]="item.icon" [size]="24" />
          <span>{{ item.label }}</span>
        </a>
      }
      <a routerLink="/report" routerLinkActive="active" class="tab report" aria-label="Report a mess">
        <span class="fab"><app-icon name="plus" [size]="30" [stroke]="2.8" /></span>
        <span>Report</span>
      </a>
      @for (item of right; track item.path) {
        <a [routerLink]="item.path" routerLinkActive="active" class="tab">
          <app-icon [name]="item.icon" [size]="24" />
          <span>{{ item.label }}</span>
        </a>
      }
    </nav>
  `,
  styles: `
    :host {
      position: fixed;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 1000;
      display: flex;
      justify-content: center;
      padding: 0 12px calc(10px + var(--safe-bottom));
      pointer-events: none;
    }
    nav {
      pointer-events: auto;
      width: 100%;
      max-width: calc(var(--col) - 24px);
      height: var(--nav-h);
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      align-items: end;
      padding: 0 6px 10px;
      border-radius: 28px;
      background: rgba(255, 255, 255, 0.86);
      backdrop-filter: blur(18px) saturate(1.6);
      -webkit-backdrop-filter: blur(18px) saturate(1.6);
      border: 1px solid rgba(255, 255, 255, 0.9);
      box-shadow: var(--shadow-lg);
    }
    .tab {
      min-height: var(--tap);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 3px;
      font-size: 11.5px;
      font-weight: 800;
      color: var(--ink-2);
      transition: color 0.2s;
    }
    .tab.active {
      color: var(--brand-600);
    }
    .report {
      color: var(--brand-900);
    }
    .fab {
      width: 60px;
      height: 60px;
      margin-top: -34px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      color: #fff;
      background: radial-gradient(circle at 35% 25%, var(--brand-500), var(--brand-700));
      border: 4px solid #fff;
      box-shadow: 0 10px 24px rgba(31, 138, 70, 0.45);
      transition: transform 0.15s;
    }
    .report:active .fab {
      transform: scale(0.94);
    }
  `,
})
export class BottomNavComponent {
  protected readonly left: NavItem[] = [
    { path: '/map', label: 'Map', icon: 'map' },
    { path: '/leaderboard', label: 'Leaderboard', icon: 'trophy' },
  ];
  protected readonly right: NavItem[] = [
    { path: '/shop', label: 'Shop', icon: 'store' },
    { path: '/profile', label: 'Profile', icon: 'user' },
  ];
}
