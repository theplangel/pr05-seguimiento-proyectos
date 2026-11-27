import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { normalizar } from './proyectos.service';

export type RolUsuario = 'LIDER' | 'INTEGRANTE';

export interface Sesion {
  usuario: string;
  rol: RolUsuario;
}

/**
 * Sesión HTTP Basic SOLO EN MEMORIA: la credencial no se guarda en localStorage/sessionStorage ni en cookies,
 * de modo que un XSS no puede leerla del almacenamiento y al cerrar o recargar la pestaña se pierde.
 * La autorización real la impone el servidor; aquí el rol solo decide qué botones se muestran.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private credencial: string | null = null;
  private actual: Sesion | null = null;

  constructor(private readonly http: HttpClient) {}

  get sesion(): Sesion | null {
    return this.actual;
  }

  /** Valor del encabezado Authorization (o null si no hay sesión). */
  get autorizacion(): string | null {
    return this.credencial ? `Basic ${this.credencial}` : null;
  }

  esLider(): boolean {
    return this.actual?.rol === 'LIDER';
  }

  login(usuario: string, clave: string): Observable<Sesion> {
    const credencial = btoa(unescape(encodeURIComponent(`${usuario}:${clave}`))); // UTF-8 seguro
    const headers = new HttpHeaders({ Authorization: `Basic ${credencial}` });
    return this.http.get<Sesion>('/api/auth/yo', { headers }).pipe(
      tap((s) => {
        this.credencial = credencial;
        this.actual = { usuario: s.usuario, rol: s.rol };
      }),
      map((s) => ({ usuario: s.usuario, rol: s.rol })),
      catchError(normalizar),
    );
  }

  cerrarSesion(): void {
    this.credencial = null;
    this.actual = null;
  }
}
