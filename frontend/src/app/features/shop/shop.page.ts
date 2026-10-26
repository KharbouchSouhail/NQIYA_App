import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { firstValueFrom, of } from 'rxjs';

import { Voucher, WalletItem } from '../../core/models/models';
import { ShopService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { errorMessage } from '../../core/services/error-message';
import { ToastService } from '../../core/services/toast.service';
import { IconComponent } from '../../shared/components/icon.component';
import { HoldButtonComponent } from './hold-button.component';

const CATEGORY_EMOJI: Record<string, string> = {
  CAFE: '☕',
  FOOD: '🍕',
  PLANTS: '🪴',
  BOOKS: '📚',
  SPORT: '🏄',
  CINEMA: '🎬',
};

@Component({
  selector: 'app-shop-page',
  imports: [DatePipe, RouterLink, IconComponent, HoldButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shop.page.html',
  styleUrl: './shop.page.scss',
})
export class ShopPage {
  private readonly shop = inject(ShopService);
  private readonly toast = inject(ToastService);
  protected readonly auth = inject(AuthService);

  protected readonly tab = signal<'catalogue' | 'wallet'>('catalogue');
  protected readonly buying = signal<number | null>(null);
  protected readonly redeeming = signal<WalletItem | null>(null);

  protected readonly catalogue = rxResource({ stream: () => this.shop.catalogue() });
  protected readonly wallet = rxResource({
    params: () => this.auth.isLoggedIn(),
    stream: ({ params: loggedIn }) => (loggedIn ? this.shop.wallet() : of([] as WalletItem[])),
  });

  protected readonly coins = computed(() => this.auth.me()?.coins ?? 0);
  protected readonly activeCount = computed(() => (this.wallet.value() ?? []).filter((w) => w.status === 'ACTIVE').length);

  protected emoji(category: string): string {
    return CATEGORY_EMOJI[category] ?? '🎁';
  }

  protected async buy(v: Voucher): Promise<void> {
    if (!confirm(`Buy "${v.title}" at ${v.partnerName} for ${v.costCoins} coins?`)) return;
    this.buying.set(v.id);
    try {
      const res = await firstValueFrom(this.shop.buy(v.id));
      this.auth.patchMe({ coins: res.coinsLeft });
      this.wallet.reload();
      this.toast.success('Bought! Find it in your wallet');
      this.tab.set('wallet');
    } catch (e) {
      this.toast.error(errorMessage(e));
    } finally {
      this.buying.set(null);
    }
  }

  protected async redeem(item: WalletItem): Promise<void> {
    try {
      const used = await firstValueFrom(this.shop.redeem(item.id));
      this.redeeming.set(used);
      this.wallet.reload();
      this.toast.success('Voucher used. Bon appétit!');
    } catch (e) {
      this.toast.error(errorMessage(e));
    }
  }
}
