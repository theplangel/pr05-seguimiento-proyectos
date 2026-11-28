import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AlertaIot, EstadoIot, LecturaIot, ResumenIot } from './iot.models';
import { normalizar } from './proyectos.service';

const BASE = '/api/iot';

/** Lectura periódica (polling) de la fuente IoT. La ingesta la hace el dispositivo, no el navegador. */
@Injectable({ providedIn: 'root' })
export class IotService {
  constructor(private readonly http: HttpClient) {}

  estado(): Observable<EstadoIot> {
    return this.http.get<EstadoIot>(`${BASE}/estado`).pipe(catchError(normalizar));
  }

  lecturas(desde: Date, hasta: Date, limite = 2000): Observable<LecturaIot[]> {
    const params = new HttpParams()
      .set('desde', desde.toISOString())
      .set('hasta', hasta.toISOString())
      .set('limite', String(limite));
    return this.http.get<LecturaIot[]>(`${BASE}/lecturas`, { params }).pipe(catchError(normalizar));
  }

  resumen(desde: Date, hasta: Date): Observable<ResumenIot> {
    const params = new HttpParams().set('desde', desde.toISOString()).set('hasta', hasta.toISOString());
    return this.http.get<ResumenIot>(`${BASE}/resumen`, { params }).pipe(catchError(normalizar));
  }

  alertas(limite = 15): Observable<AlertaIot[]> {
    const params = new HttpParams().set('limite', String(limite));
    return this.http.get<AlertaIot[]>(`${BASE}/alertas`, { params }).pipe(catchError(normalizar));
  }
}
