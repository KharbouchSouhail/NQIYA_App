import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CheckInResult,
  CreateSpotRequest,
  FinishResult,
  MyCleanup,
  Participant,
  ReportDraft,
  SpotDetail,
  SpotStatus,
  SpotSummary,
} from '../models/models';

export interface SpotQuery {
  statuses: SpotStatus[];
  lat?: number;
  lng?: number;
  radiusKm?: number;
}

@Injectable({ providedIn: 'root' })
export class SpotService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/spots`;

  list(query: SpotQuery): Observable<SpotSummary[]> {
    let params = new HttpParams();
    query.statuses.forEach((s) => (params = params.append('status', s)));
    if (query.lat != null && query.lng != null) {
      params = params.set('lat', query.lat).set('lng', query.lng);
      if (query.radiusKm) params = params.set('radiusKm', query.radiusKm);
    }
    return this.http.get<SpotSummary[]>(this.base, { params });
  }

  get(id: number): Observable<SpotDetail> {
    return this.http.get<SpotDetail>(`${this.base}/${id}`);
  }

  analyze(photo: Blob): Observable<ReportDraft> {
    return this.http.post<ReportDraft>(`${this.base}/analyze`, photoForm(photo));
  }

  create(req: CreateSpotRequest): Observable<SpotDetail> {
    return this.http.post<SpotDetail>(this.base, req);
  }

  join(id: number): Observable<SpotDetail> {
    return this.http.post<SpotDetail>(`${this.base}/${id}/join`, null);
  }

  leave(id: number): Observable<SpotDetail> {
    return this.http.delete<SpotDetail>(`${this.base}/${id}/join`);
  }

  participants(id: number): Observable<Participant[]> {
    return this.http.get<Participant[]>(`${this.base}/${id}/participants`);
  }

  checkIn(id: number, ticketCode: string): Observable<CheckInResult> {
    return this.http.post<CheckInResult>(`${this.base}/${id}/check-in`, { ticketCode });
  }

  start(id: number): Observable<SpotDetail> {
    return this.http.post<SpotDetail>(`${this.base}/${id}/start`, null);
  }

  finish(id: number, photo: Blob): Observable<FinishResult> {
    return this.http.post<FinishResult>(`${this.base}/${id}/finish`, photoForm(photo));
  }

  myCleanups(): Observable<MyCleanup[]> {
    return this.http.get<MyCleanup[]>(`${environment.apiUrl}/me/cleanups`);
  }
}

function photoForm(photo: Blob): FormData {
  const form = new FormData();
  form.append('photo', photo, 'photo.jpg');
  return form;
}
