import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ErrorResponse } from '../models/common.model';
import { SKIP_ERROR_TOAST } from './silent-request.token';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const body = error.error as ErrorResponse | undefined;
      const message = body?.message ?? error.message ?? 'Something went wrong. Please try again.';
      const silent = req.context.get(SKIP_ERROR_TOAST);

      if (error.status === 401) {
        const wasLoggedIn = authService.isAuthenticated();
        authService.logout();
        if (wasLoggedIn) {
          snackBar.open('Your session has expired. Please sign in again.', 'Dismiss', { duration: 5000 });
        }
        router.navigate(['/login']);
      } else if (!silent) {
        if (error.status === 403) {
          snackBar.open('You do not have permission to perform this action.', 'Dismiss', { duration: 5000 });
        } else if (error.status !== 0 && !body?.fieldErrors) {
          snackBar.open(message, 'Dismiss', { duration: 5000 });
        } else if (error.status === 0) {
          snackBar.open('Unable to reach the server. Please check your connection.', 'Dismiss', { duration: 5000 });
        }
      }

      return throwError(() => error);
    })
  );
};
