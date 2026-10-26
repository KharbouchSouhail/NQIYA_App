import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthResponse, Me, RequestCodeResponse } from '../models/models';

const TOKEN_KEY = 'nqiya.token';

/** Session state: JWT + the current user profile, exposed as signals. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly api = `${environment.apiUrl}`;

  private readonly tokenSignal = signal<string | null>(readToken());
  private readonly meSignal = signal<Me | null>(null);

  readonly token = this.tokenSignal.asReadonly();
  readonly me = this.meSignal.asReadonly();
  readonly isLoggedIn = computed(() => this.tokenSignal() !== null);
  readonly isAdmin = computed(() => this.meSignal()?.role === 'ADMIN');

  /** Loads the profile at startup when a token is stored. Never throws. */
  async restore(): Promise<void> {
    if (!this.tokenSignal()) return;
    try {
      await this.refreshMe();
    } catch {
      this.clear();
    }
  }

  requestCode(email: string): Promise<RequestCodeResponse> {
    return firstValueFrom(this.http.post<RequestCodeResponse>(`${this.api}/auth/request-code`, { email }));
  }

  async verify(email: string, code: string): Promise<AuthResponse> {
    const res = await firstValueFrom(this.http.post<AuthResponse>(`${this.api}/auth/verify`, { email, code }));
    this.tokenSignal.set(res.token);
    writeToken(res.token);
    this.meSignal.set(res.user);
    return res;
  }

  async refreshMe(): Promise<Me> {
    const me = await firstValueFrom(this.http.get<Me>(`${this.api}/me`));
    this.meSignal.set(me);
    return me;
  }

  async updateName(displayName: string): Promise<Me> {
    const me = await firstValueFrom(this.http.patch<Me>(`${this.api}/me`, { displayName }));
    this.meSignal.set(me);
    return me;
  }

  /** Optimistically patch wallet numbers after a payout or purchase. */
  patchMe(patch: Partial<Me>): void {
    const current = this.meSignal();
    if (current) this.meSignal.set({ ...current, ...patch });
  }

  logout(): void {
    this.clear();
    void this.router.navigateByUrl('/map');
  }

  /** Called by the error interceptor on 401. */
  clear(): void {
    this.tokenSignal.set(null);
    this.meSignal.set(null);
    writeToken(null);
  }
}

function readToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

function writeToken(token: string | null): void {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    /* storage unavailable (private mode) — session lives in memory only */
  }
}
