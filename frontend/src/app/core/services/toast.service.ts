import { Injectable, signal } from '@angular/core';

export type ToastKind = 'success' | 'error' | 'info';

export interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}

/** A toast for every action — success, error or info. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  private readonly items = signal<Toast[]>([]);
  readonly toasts = this.items.asReadonly();

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string): void {
    this.push('error', message, 5000);
  }

  info(message: string): void {
    this.push('info', message);
  }

  dismiss(id: number): void {
    this.items.update((list) => list.filter((t) => t.id !== id));
  }

  private push(kind: ToastKind, message: string, ttl = 3200): void {
    const id = this.nextId++;
    this.items.update((list) => [...list.slice(-2), { id, kind, message }]);
    setTimeout(() => this.dismiss(id), ttl);
  }
}
