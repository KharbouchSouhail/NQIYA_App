import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { HistoryItem } from '../../core/models/models';
import { RANK_STYLES } from '../../core/models/ranks';
import { ConfigService, HistoryService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { errorMessage } from '../../core/services/error-message';
import { SpotService } from '../../core/services/spot.service';
import { ToastService } from '../../core/services/toast.service';
import { IconComponent, IconName } from '../../shared/components/icon.component';
import { RankBadgeComponent } from '../../shared/components/rank-badge.component';
import { TicketSheetComponent } from '../spot/ticket-sheet.component';

const HISTORY_ICONS: Record<HistoryItem['type'], IconName> = {
  CLEANUP: 'leaf',
  ORGANIZER_BONUS: 'star',
  RANK_UP_GIFT: 'gift',
  PURCHASE: 'store',
};

@Component({
  selector: 'app-profile-page',
  imports: [DatePipe, DecimalPipe, FormsModule, RouterLink, IconComponent, RankBadgeComponent, TicketSheetComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './profile.page.html',
  styleUrl: './profile.page.scss',
})
export class ProfilePage implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly historyApi = inject(HistoryService);
  private readonly spots = inject(SpotService);
  private readonly toast = inject(ToastService);

  protected readonly history = rxResource({ stream: () => this.historyApi.history() });
  protected readonly cleanups = rxResource({ stream: () => this.spots.myCleanups() });
  protected readonly config = toSignal(inject(ConfigService).config$);

  protected readonly tab = signal<'history' | 'cleanups' | 'ranks'>('history');
  protected readonly showTicket = signal(false);
  protected readonly editing = signal(false);
  protected nameDraft = '';

  protected readonly rankStyle = computed(() => {
    const me = this.auth.me();
    return me ? RANK_STYLES[me.rank.current] : null;
  });
  protected readonly rankStyles = RANK_STYLES;
  protected readonly historyIcons = HISTORY_ICONS;

  ngOnInit(): void {
    void this.auth.refreshMe().catch(() => undefined);
  }

  protected startEdit(): void {
    this.nameDraft = this.auth.me()?.displayName ?? '';
    this.editing.set(true);
  }

  protected async saveName(): Promise<void> {
    const name = this.nameDraft.trim();
    if (name.length < 2) {
      this.toast.error('Your name needs at least 2 characters');
      return;
    }
    try {
      await this.auth.updateName(name);
      this.editing.set(false);
      this.toast.success('Name updated');
    } catch (e) {
      this.toast.error(errorMessage(e));
    }
  }
}
