import { HttpErrorResponse } from '@angular/common/http';

import { ApiProblem } from '../models/models';

/** Turns any error into a short, plain-English message for a toast. */
export function errorMessage(err: unknown, fallback = 'Something went wrong, please try again'): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) return 'No connection. Check your internet and try again';
    const problem = err.error as ApiProblem | null;
    if (problem?.errors) {
      const [field, msg] = Object.entries(problem.errors)[0] ?? [];
      if (field) return `${field}: ${msg}`;
    }
    if (problem?.detail) return problem.detail;
    if (err.status === 401) return 'Please log in again';
    return fallback;
  }
  if (err instanceof Error && err.message) return err.message;
  return fallback;
}
