import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { ReportDraft, Visibility } from '../../core/models/models';
import { errorMessage } from '../../core/services/error-message';
import { GeoFix, GeolocationService } from '../../core/services/geolocation.service';
import { ImageService } from '../../core/services/image.service';
import { SpotService } from '../../core/services/spot.service';
import { ToastService } from '../../core/services/toast.service';
import { IconComponent } from '../../shared/components/icon.component';

type Step = 'snap' | 'checking' | 'details';

const MAX_ACCURACY_M = 100;

@Component({
  selector: 'app-report-page',
  imports: [ReactiveFormsModule, IconComponent, DecimalPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './report.page.html',
  styleUrl: './report.page.scss',
})
export class ReportPage {
  private readonly spots = inject(SpotService);
  private readonly images = inject(ImageService);
  private readonly geo = inject(GeolocationService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly step = signal<Step>('snap');
  protected readonly previewUrl = signal<string | null>(null);
  protected readonly draft = signal<ReportDraft | null>(null);
  protected readonly rejection = signal<string | null>(null);
  protected readonly fix = signal<GeoFix | null>(null);
  protected readonly locating = signal(false);
  protected readonly publishing = signal(false);

  protected readonly fixOk = computed(() => {
    const f = this.fix();
    return !!f && f.accuracy <= MAX_ACCURACY_M;
  });

  protected readonly steps = [
    { id: 1, label: 'Snap it' },
    { id: 2, label: 'AI check' },
    { id: 3, label: 'Details' },
    { id: 4, label: 'Publish' },
  ];
  protected readonly stepIndex = computed(() => ({ snap: 1, checking: 2, details: 3 })[this.step()]);

  protected readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(80)]],
    description: ['', [Validators.maxLength(400)]],
    visibility: ['PUBLIC' as Visibility],
    meetingAt: [defaultMeeting()],
    anonymous: [false],
  });

  protected readonly minMeeting = toLocalInput(new Date());

  constructor() {
    inject(DestroyRef).onDestroy(() => {
      const url = this.previewUrl();
      if (url) URL.revokeObjectURL(url);
    });
  }

  protected async onPhoto(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;

    this.rejection.set(null);
    this.step.set('checking');
    void this.pin(); // a fresh GPS fix where you stand, in parallel with the AI check

    try {
      const blob = await this.images.compress(file);
      const old = this.previewUrl();
      if (old) URL.revokeObjectURL(old);
      this.previewUrl.set(URL.createObjectURL(blob));

      const draft = await firstValueFrom(this.spots.analyze(blob));
      if (!draft.accepted) {
        this.rejection.set(draft.reason);
        this.step.set('snap');
        return;
      }
      this.draft.set(draft);
      this.form.patchValue({ title: draft.title ?? '', description: draft.description ?? '' });
      this.step.set('details');
      this.toast.success('AI check passed — looks like a mess to clean!');
    } catch (e) {
      this.step.set('snap');
      this.toast.error(errorMessage(e, 'The photo check failed, please try again'));
    }
  }

  protected async pin(): Promise<void> {
    this.locating.set(true);
    try {
      this.fix.set(await this.geo.current());
    } catch (e) {
      this.toast.error(errorMessage(e));
    } finally {
      this.locating.set(false);
    }
  }

  protected setVisibility(v: Visibility): void {
    this.form.controls.visibility.setValue(v);
  }

  protected async publish(): Promise<void> {
    const draft = this.draft();
    const fix = this.fix();
    if (!draft?.photoId || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!fix || fix.accuracy > MAX_ACCURACY_M) {
      this.toast.error('We need a GPS fix accurate to 100 m. Step outside and retry');
      return;
    }
    const v = this.form.getRawValue();
    const isPublic = v.visibility === 'PUBLIC';
    if (isPublic && !v.meetingAt) {
      this.toast.error('Pick a meeting time for your cleanup');
      return;
    }

    this.publishing.set(true);
    try {
      const spot = await firstValueFrom(
        this.spots.create({
          photoId: draft.photoId,
          title: v.title.trim(),
          description: v.description.trim() || null,
          latitude: fix.lat,
          longitude: fix.lng,
          accuracyMeters: Math.round(fix.accuracy),
          visibility: v.visibility,
          anonymous: !isPublic && v.anonymous,
          meetingAt: isPublic ? new Date(v.meetingAt).toISOString() : null,
        }),
      );
      this.toast.success(isPublic ? 'Yallah! Your cleanup is on the map' : 'Solo cleanup saved. Go get it!');
      await this.router.navigate(['/spots', spot.id], { replaceUrl: true });
    } catch (e) {
      this.toast.error(errorMessage(e));
    } finally {
      this.publishing.set(false);
    }
  }
}

function defaultMeeting(): string {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  d.setHours(10, 0, 0, 0);
  return toLocalInput(d);
}

function toLocalInput(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
