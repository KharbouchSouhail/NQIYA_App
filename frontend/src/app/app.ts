import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';

import { AuthService } from './core/services/auth.service';
import { BottomNavComponent } from './shared/components/bottom-nav.component';
import { CelebrationOverlayComponent } from './shared/components/celebration-overlay.component';
import { SplashComponent } from './shared/components/splash.component';
import { ToastContainerComponent } from './shared/components/toast-container.component';

const NO_NAV_ROUTES = ['/login'];

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, BottomNavComponent, ToastContainerComponent, CelebrationOverlayComponent, SplashComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (showSplash()) {
      <app-splash />
    }
    <main>
      <router-outlet />
    </main>
    @if (showNav()) {
      <app-bottom-nav />
    }
    <app-toasts />
    <app-celebration />
  `,
})
export class App implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly showSplash = signal(true);

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: '/' },
  );

  protected readonly showNav = computed(() => !NO_NAV_ROUTES.some((r) => this.url().startsWith(r)));

  ngOnInit(): void {
    const minimum = new Promise((r) => setTimeout(r, 1400));
    void Promise.all([this.auth.restore(), minimum]).then(() => this.showSplash.set(false));
  }
}
