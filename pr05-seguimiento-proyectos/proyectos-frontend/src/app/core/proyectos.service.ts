import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApiError, Estado, Proyecto, ProyectoRequest, Salud } from './models';

/** Base relativa: en desarrollo la resuelve proxy.conf.json y en Docker el proxy de nginx. */
export const API_BASE = '/api';

@Injectable({ providedIn: 'root' })
export class ProyectosService {
  constructor(private readonly http: HttpClient) {}

  salud(): Observable<Salud> {
    return this.http.get<Salud>(`${API_BASE}/health`).pipe(catchError(normalizar));
  }

  listar(estado?: Estado | ''): Observable<Proyecto[]> {
    let params = new HttpParams();
    if (estado) {
      params = params.set('estado', estado);
    }
    return this.http.get<Proyecto[]>(`${API_BASE}/proyectos`, { params }).pipe(catchError(normalizar));
  }

  obtener(id: number): Observable<Proyecto> {
    return this.http.get<Proyecto>(`${API_BASE}/proyectos/${id}`).pipe(catchError(normalizar));
  }

  crear(datos: ProyectoRequest): Observable<Proyecto> {
    return this.http.post<Proyecto>(`${API_BASE}/proyectos`, datos).pipe(catchError(normalizar));
  }

  cambiarEstado(id: number, estado: Estado): Observable<Proyecto> {
    return this.http
      .patch<Proyecto>(`${API_BASE}/proyectos/${id}/estado`, { estado })
      .pipe(catchError(normalizar));
  }
}

/** Convierte cualquier fallo HTTP en un ApiError con mensaje legible para el usuario. */
export function normalizar(err: HttpErrorResponse): Observable<never> {
  if (err.status === 0) {
    return throwError(() => ({
      status: 0,
      mensaje: 'No se pudo conectar con el servidor. Verifica que la API esté en ejecución.',
    } as ApiError));
  }
  const cuerpo = err.error as Partial<ApiError> | null;
  const apiError: ApiError = {
    status: err.status,
    mensaje: (cuerpo && cuerpo.mensaje) || `Error inesperado (HTTP ${err.status})`,
    campos: cuerpo && cuerpo.campos ? cuerpo.campos : null,
    ruta: cuerpo?.ruta,
  };
  return throwError(() => apiError);
}
