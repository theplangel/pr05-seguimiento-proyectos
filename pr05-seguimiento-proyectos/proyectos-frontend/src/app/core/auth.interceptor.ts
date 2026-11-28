import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Añade Authorization solo a llamadas a la propia API y cierra la sesión si el servidor responde 401. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const autorizacion = auth.autorizacion;
  const llamadaApi = req.url.startsWith('/api/');
  const peticion =
    autorizacion && llamadaApi && !req.headers.has('Authorization')
      ? req.clone({ setHeaders: { Authorization: autorizacion } })
      : req;
  return next(peticion).pipe(
    catchError((e: HttpErrorResponse) => {
      if (e.status === 401 && autorizacion && req.url !== '/api/auth/yo') {
        auth.cerrarSesion(); // credencial vencida o revocada
      }
      return throwError(() => e);
    }),
  );
};
