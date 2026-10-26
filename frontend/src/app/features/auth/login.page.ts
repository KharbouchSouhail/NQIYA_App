import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { errorMessage } from '../../core/services/error-message';
import { ToastService } from '../../core/services/toast.service';
import { IconComponent } from '../../shared/components/icon.component';
import { LogoComponent } from '../../shared/components/logo.component';

/** Passwordless login: email → 6-digit code. */
@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, LogoComponent, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page login">
      <a routerLink="/map" class="icon-btn back" aria-label="Back to the map"><app-icon name="chevron-left" /></a>

      <section class="hero">
        <app-logo [size]="56" />
        <h1>Spot a mess.<br />Clean it together.<br /><em>Level up.</em></h1>
        <p class="muted">No password. We'll email you a 6-digit code.</p>
      </section>

      @if (step() === 'email') {
        <form class="card form pop-in" [formGroup]="emailForm" (ngSubmit)="sendCode()">
          <div class="field">
            <label for="email">Email</label>
            <input
              id="email"
              class="input"
              type="email"
              formControlName="email"
              autocomplete="email"
              inputmode="email"
              placeholder="you@example.com"
            />
            @if (emailForm.controls.email.touched && emailForm.controls.email.invalid) {
              <span class="error">Enter a valid email</span>
            }
          </div>
          <button class="btn btn-primary block lg" type="submit" [disabled]="busy()">
            @if (busy()) {
              <span class="spinner"></span>
            }
            Send me a code
          </button>
        </form>
      } @else {
        <form class="card form pop-in" [formGroup]="codeForm" (ngSubmit)="verify()">
          <p class="small muted">
            Code sent to <strong>{{ email() }}</strong>.
            <button type="button" class="link" (click)="step.set('email')">Change</button>
          </p>
          @if (devCode()) {
            <p class="pill gold">Dev mode code: {{ devCode() }}</p>
          }
          <div class="field">
            <label for="code">6-digit code</label>
            <input
              id="code"
              class="input code"
              type="text"
              formControlName="code"
              inputmode="numeric"
              autocomplete="one-time-code"
              maxlength="6"
              placeholder="••••••"
            />
          </div>
          <button class="btn btn-primary block lg" type="submit" [disabled]="busy() || codeForm.invalid">
            @if (busy()) {
              <span class="spinner"></span>
            }
            Yallah, let's go
          </button>
          <button type="button" class="btn btn-ghost block" (click)="sendCode()" [disabled]="busy()">Resend code</button>
        </form>
      }
    </div>
  `,
  styles: `
    .login {
      padding-bottom: 32px;
      justify-content: center;
      position: relative;
    }
    .back {
      position: absolute;
      top: calc(16px + var(--safe-top));
      left: 16px;
    }
    .hero {
      display: flex;
      flex-direction: column;
      gap: 14px;
      margin-bottom: 8px;
    }
    h1 {
      font-size: 34px;
      color: var(--brand-900);
      em {
        font-style: normal;
        color: var(--brand-600);
      }
    }
    .form {
      display: flex;
      flex-direction: column;
      gap: 14px;
      padding: 20px;
    }
    .link {
      border: 0;
      background: none;
      color: var(--brand-700);
      font-weight: 800;
      cursor: pointer;
      padding: 0 4px;
    }
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder).nonNullable;

  /** Return URL, bound from the `?next=` query parameter. */
  readonly next = input<string>();

  protected readonly step = signal<'email' | 'code'>('email');
  protected readonly busy = signal(false);
  protected readonly email = signal('');
  protected readonly devCode = signal<string | null>(null);

  protected readonly emailForm = this.fb.group({ email: ['', [Validators.required, Validators.email]] });
  protected readonly codeForm = this.fb.group({ code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]] });

  protected async sendCode(): Promise<void> {
    if (this.emailForm.invalid) {
      this.emailForm.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    try {
      const email = this.emailForm.getRawValue().email.trim();
      const res = await this.auth.requestCode(email);
      this.email.set(email);
      this.devCode.set(res.devCode);
      this.step.set('code');
      this.toast.success('Code sent! Check your inbox');
    } catch (e) {
      this.toast.error(errorMessage(e));
    } finally {
      this.busy.set(false);
    }
  }

  protected async verify(): Promise<void> {
    this.busy.set(true);
    try {
      const res = await this.auth.verify(this.email(), this.codeForm.getRawValue().code);
      this.toast.success(res.newUser ? `Marhba ${res.user.displayName}! Welcome to NQIYA` : `Welcome back, ${res.user.displayName}`);
      const target = this.next();
      await this.router.navigateByUrl(target && target.startsWith('/') && !target.startsWith('//') ? target : '/map');
    } catch (e) {
      this.toast.error(errorMessage(e));
      this.codeForm.reset();
    } finally {
      this.busy.set(false);
    }
  }
}
