import { Injectable, signal } from '@angular/core';
import confetti from 'canvas-confetti';

import { RewardSummary } from '../models/models';

/** Drives the reward overlay + confetti shown after a verified cleanup. */
@Injectable({ providedIn: 'root' })
export class CelebrationService {
  readonly current = signal<RewardSummary | null>(null);

  celebrate(reward: RewardSummary): void {
    this.current.set(reward);
    if (prefersReducedMotion()) return;
    const colors = ['#1f8a46', '#7fd18f', '#f5b82e', '#ffffff'];
    void confetti({ particleCount: 140, spread: 80, origin: { y: 0.65 }, colors });
    if (reward.rankAfter !== reward.rankBefore) {
      setTimeout(() => void confetti({ particleCount: 220, spread: 120, startVelocity: 45, origin: { y: 0.5 }, colors }), 900);
    }
  }

  close(): void {
    this.current.set(null);
  }
}

export function prefersReducedMotion(): boolean {
  return typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
}
