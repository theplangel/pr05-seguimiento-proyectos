import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { EMPTY, Subscription, forkJoin, timer } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { AlertaIot, EstadoIot, LecturaIot, RANGOS, ResumenIot } from '../core/iot.models';
import { IotService } from '../core/iot.service';
import { ApiError } from '../core/models';
import { GraficaIot, construirGrafica } from './iot.grafica';

/** Periodo de lectura del dashboard (igual a la frecuencia del sensor: 5 s). */
export const PERIODO_MS = 5000;

@Component({
  selector: 'app-dashboard-iot',
  standalone: true,
  templateUrl: './dashboard-iot.component.html',
})
export class DashboardIotComponent implements OnInit, OnDestroy {
  readonly rangos = RANGOS;
  minutos = 60;
  estado: EstadoIot | null = null;
  lecturas: LecturaIot[] = [];
  resumen: ResumenIot | null = null;
  alertas: AlertaIot[] = [];
  grafica: GraficaIot | null = null;
  error = '';
  cargando = true;
  actualizado: Date | null = null;

  private readonly servicio = inject(IotService);
  private sub?: Subscription;

  ngOnInit(): void {
    this.iniciar();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  cambiarRango(valor: string): void {
    this.minutos = Number(valor);
    this.cargando = true;
    this.iniciar();
  }

  /** Segundos sin recibir datos, medidos con el reloj del servidor. */
  get segundosSinDatos(): number | null {
    if (!this.estado || !this.estado.ultimaRecepcion) {
      return null;
    }
    return Math.max(0, Math.floor((Date.parse(this.estado.ahora) - Date.parse(this.estado.ultimaRecepcion)) / 1000));
  }

  get ultimasLecturas(): LecturaIot[] {
    return this.lecturas.slice(-8).reverse();
  }

  hora(iso: string): string {
    return new Date(iso).toLocaleTimeString('es-MX', { hour12: false });
  }

  etiquetaNivel(): string {
    switch (this.estado?.nivel) {
      case 'OK': return '● Normal';
      case 'ADVERTENCIA': return '▲ Advertencia';
      case 'CRITICO': return '■ Crítico';
      default: return '○ Sin datos';
    }
  }

  etiquetaConexion(): string {
    switch (this.estado?.conexion) {
      case 'CONECTADO': return 'Conectado';
      case 'DESCONECTADO': return 'Desconectado';
      default: return 'Sin datos';
    }
  }

  private iniciar(): void {
    this.sub?.unsubscribe();
    this.sub = timer(0, PERIODO_MS)
      .pipe(switchMap(() => this.consultar()))
      .subscribe((d) => this.aplicar(d));
  }

  private consultar() {
    const ahora = Date.now();
    const desde = new Date(ahora - this.minutos * 60_000);
    const hasta = new Date(ahora + 60_000); // holgura por diferencia de relojes
    return forkJoin({
      estado: this.servicio.estado(),
      lecturas: this.servicio.lecturas(desde, hasta),
      resumen: this.servicio.resumen(desde, hasta),
      alertas: this.servicio.alertas(15),
    }).pipe(
      catchError((e: ApiError) => {
        this.error = e.mensaje; // se conserva lo último mostrado y se reintenta en el siguiente ciclo
        this.cargando = false;
        return EMPTY;
      }),
    );
  }

  private aplicar(d: { estado: EstadoIot; lecturas: LecturaIot[]; resumen: ResumenIot; alertas: AlertaIot[] }): void {
    this.estado = d.estado;
    this.lecturas = d.lecturas;
    this.resumen = d.resumen;
    this.alertas = d.alertas;
    this.grafica = construirGrafica(d.lecturas, d.estado.umbralAdvertencia, d.estado.umbralCritico,
      d.estado.desconexionSegundos);
    this.error = '';
    this.cargando = false;
    this.actualizado = new Date();
  }
}
