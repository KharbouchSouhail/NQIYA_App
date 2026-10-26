import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ActionLog,
  AdminSpot,
  AdminStats,
  AdminUser,
  HistoryItem,
  Leaderboard,
  Page,
  PublicConfig,
  PurchaseResult,
  Role,
  Voucher,
  WalletItem,
} from '../models/models';

const API = environment.apiUrl;

@Injectable({ providedIn: 'root' })
export class LeaderboardService {
  private readonly http = inject(HttpClient);

  get(limit = 50): Observable<Leaderboard> {
    return this.http.get<Leaderboard>(`${API}/leaderboard`, { params: { limit } });
  }
}

@Injectable({ providedIn: 'root' })
export class ShopService {
  private readonly http = inject(HttpClient);

  catalogue(): Observable<Voucher[]> {
    return this.http.get<Voucher[]>(`${API}/shop/vouchers`);
  }

  buy(voucherId: number): Observable<PurchaseResult> {
    return this.http.post<PurchaseResult>(`${API}/shop/vouchers/${voucherId}/buy`, null);
  }

  wallet(): Observable<WalletItem[]> {
    return this.http.get<WalletItem[]>(`${API}/wallet`);
  }

  redeem(purchaseId: number): Observable<WalletItem> {
    return this.http.post<WalletItem>(`${API}/wallet/${purchaseId}/redeem`, null);
  }
}

@Injectable({ providedIn: 'root' })
export class HistoryService {
  private readonly http = inject(HttpClient);

  history(): Observable<HistoryItem[]> {
    return this.http.get<HistoryItem[]>(`${API}/me/history`);
  }
}

@Injectable({ providedIn: 'root' })
export class ConfigService {
  private readonly http = inject(HttpClient);

  /** Game rules rarely change: fetched once per session. */
  readonly config$ = this.http.get<PublicConfig>(`${API}/config`).pipe(shareReplay(1));
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API}/admin`;

  stats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.base}/stats`);
  }

  spots(page = 0): Observable<Page<AdminSpot>> {
    return this.http.get<Page<AdminSpot>>(`${this.base}/spots`, { params: { page } });
  }

  closeSpot(id: number, reason?: string): Observable<void> {
    return this.http.post<void>(`${this.base}/spots/${id}/close`, { reason });
  }

  reopenSpot(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/spots/${id}/reopen`, {});
  }

  deleteSpot(id: number, reason?: string): Observable<void> {
    let params = new HttpParams();
    if (reason) params = params.set('reason', reason);
    return this.http.delete<void>(`${this.base}/spots/${id}`, { params });
  }

  users(q = '', page = 0): Observable<Page<AdminUser>> {
    return this.http.get<Page<AdminUser>>(`${this.base}/users`, { params: { q, page } });
  }

  setRole(userId: number, role: Role): Observable<void> {
    return this.http.put<void>(`${this.base}/users/${userId}/role`, { role });
  }

  log(page = 0): Observable<Page<ActionLog>> {
    return this.http.get<Page<ActionLog>>(`${this.base}/log`, { params: { page } });
  }
}
