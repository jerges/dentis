import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';
import { getHttpErrorMessage } from '../utils/http-error.util';

export const SKIP_GLOBAL_ERROR_HANDLER = new HttpContextToken<boolean>(() => false);

export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const snack = inject(MatSnackBar);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || req.context.get(SKIP_GLOBAL_ERROR_HANDLER)) {
        return throwError(() => error);
      }

      snack.open(getHttpErrorMessage(error), 'Cerrar', { duration: 5500 });
      return throwError(() => error);
    })
  );
};


