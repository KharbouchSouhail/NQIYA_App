import { DatePipe, DecimalPipe } from '@angular/common';
import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { Subscription } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SpotStatus, SpotSummary } from '../../core/models/models';
import { STATUS_STYLES } from '../../core/models/ranks';
import { AuthService } from '../../core/services/auth.service';
import { errorMessage } from '../../core/services/error-message';
import { GeolocationService } from '../../core/services/geolocation.service';
import { SpotService } from '../../core/services/spot.service';
import { ToastService } from '../../core/services/toast.service';
import { IconComponent } from '../../shared/components/icon.component';
import { LogoComponent } from '../../shared/components/logo.component';
import { pinHtml } from '../../shared/components/status-pin';

const ALL_STATUSES: SpotStatus[] = ['DIRTY', 'CLEANING', 'CLEAN'];

@Component({
  selector: 'app-map-page',
  imports: [IconComponent, LogoComponent, RouterLink, DatePipe, DecimalPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './map.page.html',
  styleUrl: './map.page.scss',
})
export class MapPage {
  private readonly spotsApi = inject(SpotService);
  private readonly geo = inject(GeolocationService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  private readonly mapEl = viewChild.required<ElementRef<HTMLDivElement>>('map');
  private map?: L.Map;
  private markers = L.layerGroup();
  private meMarker?: L.CircleMarker;

  protected readonly statuses = ALL_STATUSES;
  protected readonly statusStyles = STATUS_STYLES;
  protected readonly active = signal<Set<SpotStatus>>(new Set(ALL_STATUSES));
  protected readonly radiusKm = signal(10);
  protected readonly showRadius = signal(false);
  protected readonly myPosition = signal<{ lat: number; lng: number } | null>(null);
  protected readonly spots = signal<SpotSummary[]>([]);
  protected readonly selected = signal<SpotSummary | null>(null);
  protected readonly loading = signal(false);
  protected readonly locating = signal(false);

  constructor() {
    afterNextRender(() => {
      this.initMap();
      void this.locate(false);
    });

    // reload whenever filters or position change
    effect((onCleanup) => {
      const sub = this.load([...this.active()], this.myPosition(), this.radiusKm());
      onCleanup(() => sub.unsubscribe());
    });

    effect(() => this.renderMarkers(this.spots()));

    inject(DestroyRef).onDestroy(() => this.map?.remove());
  }

  protected toggle(status: SpotStatus): void {
    this.active.update((set) => {
      const next = new Set(set);
      if (next.has(status) && next.size > 1) next.delete(status);
      else next.add(status);
      return next;
    });
  }

  protected setRadius(event: Event): void {
    this.radiusKm.set(Number((event.target as HTMLInputElement).value));
  }

  protected async locate(userInitiated = true): Promise<void> {
    this.locating.set(true);
    try {
      const fix = await this.geo.current(10000);
      this.myPosition.set({ lat: fix.lat, lng: fix.lng });
      this.map?.flyTo([fix.lat, fix.lng], 14, { duration: 0.8 });
      this.drawMe(fix.lat, fix.lng);
    } catch (e) {
      if (userInitiated) this.toast.error(errorMessage(e));
    } finally {
      this.locating.set(false);
    }
  }

  protected open(spot: SpotSummary): void {
    void this.router.navigate(['/spots', spot.id]);
  }

  private load(statuses: SpotStatus[], pos: { lat: number; lng: number } | null, radiusKm: number): Subscription {
    this.loading.set(true);
    return this.spotsApi
      .list({ statuses, lat: pos?.lat, lng: pos?.lng, radiusKm: pos ? radiusKm : undefined })
      .subscribe({
        next: (spots) => {
          this.spots.set(spots);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          this.toast.error(errorMessage(err, 'Could not load the map'));
        },
      });
  }

  private initMap(): void {
    const { lat, lng } = environment.defaultCenter;
    this.map = L.map(this.mapEl().nativeElement, {
      center: [lat, lng],
      zoom: 13,
      zoomControl: false,
      attributionControl: true,
    });
    L.tileLayer(environment.mapTilesUrl, {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.map);
    this.markers.addTo(this.map);
    this.map.on('click', () => this.selected.set(null));
    this.renderMarkers(this.spots());
  }

  private renderMarkers(spots: SpotSummary[]): void {
    if (!this.map) return;
    this.markers.clearLayers();
    for (const spot of spots) {
      const { html, size } = pinHtml(spot.status, spot.participantCount);
      const icon = L.divIcon({ html, className: 'nq-pin-wrap', iconSize: [size, size + 10], iconAnchor: [size / 2, size + 8] });
      L.marker([spot.latitude, spot.longitude], { icon, title: spot.title, keyboard: true })
        .on('click', (e) => {
          L.DomEvent.stopPropagation(e);
          this.selected.set(spot);
        })
        .addTo(this.markers);
    }
  }

  private drawMe(lat: number, lng: number): void {
    if (!this.map) return;
    this.meMarker?.remove();
    this.meMarker = L.circleMarker([lat, lng], {
      radius: 8,
      color: '#fff',
      weight: 3,
      fillColor: '#2f7cf6',
      fillOpacity: 1,
    }).addTo(this.map);
  }
}
