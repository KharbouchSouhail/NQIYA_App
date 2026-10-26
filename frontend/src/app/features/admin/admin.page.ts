import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';

import { AdminSpot, AdminUser } from '../../core/models/models';
import { AdminService } from '../../core/services/api.services';
import { AuthService } from '../../core/services/auth.service';
import { errorMessage } from '../../core/services/error-message';
import { ToastService } from '../../core/services/toast.service';
import { IconComponent } from '../../shared/components/icon.component';
import { RankBadgeComponent } from '../../shared/components/rank-badge.component';

/** Moderation: close / reopen / delete spots, grant admins, read the action log. */
@Component({
  selector: 'app-admin-page',
  imports: [DatePipe, FormsModule, RouterLink, IconComponent, RankBadgeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin.page.html',
  styleUrl: './admin.page.scss',
})
export class AdminPage {
  private readonly api = inject(AdminService);
  private readonly toast = inject(ToastService);
  protected readonly auth = inject(AuthService);

  protected readonly tab = signal<'spots' | 'users' | 'log'>('spots');
  protected readonly spotPage = signal(0);
  protected readonly logPage = signal(0);
  protected readonly userQuery = signal('');
  protected queryDraft = '';
  protected readonly Math = Math;

  protected readonly stats = rxResource({ stream: () => this.api.stats() });
  protected readonly spots = rxResource({ params: () => this.spotPage(), stream: ({ params }) => this.api.spots(params) });
  protected readonly users = rxResource({ params: () => this.userQuery(), stream: ({ params }) => this.api.users(params) });
  protected readonly log = rxResource({ params: () => this.logPage(), stream: ({ params }) => this.api.log(params) });

  protected search(): void {
    this.userQuery.set(this.queryDraft.trim());
  }

  protected close(s: AdminSpot): void {
    const reason = prompt(`Close "${s.title}"? Optional reason:`);
    if (reason === null) return;
    void this.act(this.api.closeSpot(s.id, reason), 'Spot closed');
  }

  protected reopen(s: AdminSpot): void {
    void this.act(this.api.reopenSpot(s.id), 'Spot reopened');
  }

  protected remove(s: AdminSpot): void {
    if (!confirm(`Delete "${s.title}" permanently? This can't be undone.`)) return;
    void this.act(this.api.deleteSpot(s.id), 'Spot deleted');
  }

  protected toggleAdmin(u: AdminUser): void {
    const role = u.role === 'ADMIN' ? 'USER' : 'ADMIN';
    if (!confirm(role === 'ADMIN' ? `Make ${u.email} an admin?` : `Remove admin rights from ${u.email}?`)) return;
    void this.act(this.api.setRole(u.id, role), role === 'ADMIN' ? 'Admin granted' : 'Admin revoked');
  }

  private async act(call: Observable<void>, success: string): Promise<void> {
    try {
      await firstValueFrom(call, { defaultValue: undefined });
      this.toast.success(success);
      this.spots.reload();
      this.users.reload();
      this.log.reload();
      this.stats.reload();
    } catch (e) {
      this.toast.error(errorMessage(e));
    }
  }
}
