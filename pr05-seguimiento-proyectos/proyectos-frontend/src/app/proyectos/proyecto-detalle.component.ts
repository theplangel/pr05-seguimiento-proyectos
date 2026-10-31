import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiError, ESTADOS, Estado, Proyecto } from '../core/models';
import { ProyectosService } from '../core/proyectos.service';

/** Detalle de un proyecto (GET /api/proyectos/{id}) con su ciclo de estados y acciones de cambio. */
@Component({
  selector: 'app-proyecto-detalle',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './proyecto-detalle.component.html',
})
export class ProyectoDetalleComponent implements OnInit {
  readonly estados = ESTADOS;
  proyecto: Proyecto | null = null;
  cargando = false;
  cambiando = false;
  /** Error al obtener el recurso (404, 400, sin conexión): reemplaza el contenido. */
  error: ApiError | null = null;
  /** Error de un cambio de estado (409, 400): se muestra sobre el detalle vigente. */
  mensajeError = '';
  mensajeOk = '';

  private id = 0;
  private readonly route = inject(ActivatedRoute);
  private readonly servicio = inject(ProyectosService);

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const crudo = params.get('id') ?? '';
      this.id = /^\d+$/.test(crudo) ? Number(crudo) : 0;
      this.proyecto = null;
      this.mensajeOk = '';
      this.mensajeError = '';
      if (this.id > 0) {
        this.cargar();
      } else {
        this.error = { status: 400, mensaje: `«${crudo}» no es un identificador de proyecto válido.` };
      }
    });
  }

  cargar(): void {
    this.cargando = true;
    this.error = null;
    this.servicio.obtener(this.id).subscribe({
      next: (p) => {
        this.proyecto = p;
        this.cargando = false;
      },
      error: (e: ApiError) => {
        this.cargando = false;
        this.proyecto = null;
        this.error = e;
      },
    });
  }

  cambiar(destino: Estado): void {
    if (!this.proyecto) {
      return;
    }
    this.mensajeOk = '';
    this.mensajeError = '';
    this.cambiando = true;
    this.servicio.cambiarEstado(this.proyecto.id, destino).subscribe({
      next: (act) => {
        this.cambiando = false;
        this.proyecto = act; // la API devuelve el recurso actualizado: no hace falta otro GET
        this.mensajeOk = `Estado actualizado a ${this.etiqueta(act.estado)}.`;
      },
      error: (e: ApiError) => {
        this.cambiando = false;
        this.mensajeError = e.mensaje;
        this.cargar(); // el estado pudo haber cambiado desde otro cliente
      },
    });
  }

  /** Posición de un estado respecto al actual en la línea de vida del proyecto. */
  paso(e: Estado): 'hecho' | 'actual' | 'pendiente' {
    const actual = this.proyecto ? ESTADOS.indexOf(this.proyecto.estado) : -1;
    const i = ESTADOS.indexOf(e);
    return i < actual ? 'hecho' : i === actual ? 'actual' : 'pendiente';
  }

  etiqueta(e: Estado): string {
    return e.replace(/_/g, ' ');
  }
}
