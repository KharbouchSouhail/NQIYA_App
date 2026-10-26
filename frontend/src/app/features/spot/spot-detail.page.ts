import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { Participant, SpotDetail } from '../../core/models/models';
import { STATUS_STYLES } from '../../core/models/ranks';
import { AuthService } from '../../core/services/auth.service';
import { CelebrationService } from '../../core/services/celebration.service';
import { errorMessage } from '../../core/services/error-message';
import { ImageService } from '../../core/services/image.service';
import { SpotService } from '../../core/services/spot.service';
import { ToastService } from '../../core/services/toast.service';
import { IconComponent } from '../../shared/components/icon.component';
import { RankBadgeComponent } from '../../shared/components/rank-badge.component';
import { QrScannerComponent } from './qr-scanner.component';
import { TicketSheetComponent } from './ticket-sheet.component';

@Component({
  selector: 'app-spot-detail-page',
  imports: [DatePipe, RouterLink, IconComponent, RankBadgeComponent, QrScannerComponent, TicketSheetComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './spot-detail.page.html',
  styleUrl: './spot-detail.page.scss',
})
export class SpotDetailPage {
  private readonly spots = inject(SpotService);
  private readonly images = inject(ImageService);
  private readonly toast = inject(ToastService);
  private readonly celebration = inject(CelebrationService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  /** Route parameter `:id`. */
  readonly id = input.required<string>();

  protected readonly spot = signal<SpotDetail | null>(null);
  protected readonly participants = signal<Participant[]>([]);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly verifying = signal(false);
  protected readonly finishError = signal<string | null>(null);
  protected readonly showTicket = signal(false);
  protected readonly showScanner = signal(false);
  protected readonly statusStyles = STATUS_STYLES;

  protected readonly directionsUrl = computed(() => {
    const s = this.spot();
    return s ? `https://www.google.com/maps/dir/?api=1&destination=${s.latitude},${s.longitude}` : '#';
  });

  constructor() {
    effect(() => void this.load(Number(this.id())));
  }

  protected async load(id: number): Promise<void> {
    this.loading.set(true);
    try {
      const spot = await firstValueFrom(this.spots.get(id));
      this.spot.set(spot);
      if (spot.viewer.organizer || (spot.viewer.authenticated && spot.visibility === 'PUBLIC')) {
        this.participants.set(await firstValueFrom(this.spots.participants(id)));
      }
    } catch (e) {
      this.toast.error(errorMessage(e, 'This spot could not be loaded'));
      await this.router.navigateByUrl('/map');
    } finally {
      this.loading.set(false);
    }
  }

  protected join(): void {
    this.run(this.spots.join(this.spotId()), (s) => {
      this.toast.success("You're in! Show your ticket on site");
      this.spot.set(s);
      void this.reloadParticipants();
    });
  }

  protected leave(): void {
    this.run(this.spots.leave(this.spotId()), (s) => {
      this.toast.info('You left this cleanup');
      this.spot.set(s);
      void this.reloadParticipants();
    });
  }

  protected start(): void {
    this.run(this.spots.start(this.spotId()), (s) => {
      this.toast.success('Cleaning now! Good luck');
      this.spot.set(s);
      void this.reloadParticipants();
    });
  }

  protected async onScan(code: string): Promise<void> {
    try {
      const res = await firstValueFrom(this.spots.checkIn(this.spotId(), code));
      this.toast.success(
        res.alreadyCheckedIn ? `${res.volunteer.displayName} was already checked in` : `${res.volunteer.displayName} checked in!`,
      );
      this.spot.update((s) => (s ? { ...s, status: res.spotStatus, checkedInCount: res.checkedInCount } : s));
      await this.reloadParticipants();
    } catch (e) {
      this.toast.error(errorMessage(e));
    }
  }

  protected async onAfterPhoto(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    this.finishError.set(null);
    this.verifying.set(true);
    try {
      const blob = await this.images.compress(file);
      const res = await firstValueFrom(this.spots.finish(this.spotId(), blob));
      this.spot.set(res.spot);
      if (!res.passed) {
        this.finishError.set(res.reason);
        this.toast.error('Not verified yet — see why below');
        return;
      }
      this.showScanner.set(false);
      if (res.myReward) {
        this.celebration.celebrate(res.myReward);
        this.auth.patchMe({ xp: res.myReward.totalXp, coins: res.myReward.totalCoins });
      }
      this.toast.success(`Saha! ${res.paidVolunteers} volunteer(s) rewarded`);
      void this.auth.refreshMe();
    } catch (e) {
      this.toast.error(errorMessage(e));
    } finally {
      this.verifying.set(false);
    }
  }

  private spotId(): number {
    return Number(this.id());
  }

  private async reloadParticipants(): Promise<void> {
    try {
      this.participants.set(await firstValueFrom(this.spots.participants(this.spotId())));
    } catch {
      /* non-critical */
    }
  }

  private run(obs: ReturnType<SpotService['join']>, onSuccess: (s: SpotDetail) => void): void {
    this.busy.set(true);
    obs.subscribe({
      next: (s) => {
        this.busy.set(false);
        onSuccess(s);
      },
      error: (e) => {
        this.busy.set(false);
        this.toast.error(errorMessage(e));
      },
    });
  }
}
