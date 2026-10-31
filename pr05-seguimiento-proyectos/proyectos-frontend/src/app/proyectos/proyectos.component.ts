import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiError, ESTADOS, Estado, Proyecto } from '../core/models';
import { ProyectosService } from '../core/proyectos.service';
import { ProyectoFormComponent } from './proyecto-form.component';

/** Página principal: formulario de alta + lista filtrable con cambio de estado por fila. */
@Component({
  selector: 'app-proyectos',
  standalone: true,
  imports: [RouterLink, ProyectoFormComponent],
  templateUrl: './proyectos.component.html',
})
export class ProyectosComponent implements OnInit {
  readonly estados = ESTADOS;
  readonly esqueleto = [1, 2, 3];
  proyectos: Proyecto[] = [];
  filtro: Estado | '' = '';
  cargando = false;
  errorCarga = '';
  /** Id del proyecto cuyo PATCH está en curso: bloquea sus botones para evitar dobles envíos. */
  procesandoId: number | null = null;
  mensajeOk = '';
  mensajeError = '';
  ultimoCreadoId: number | null = null;

  private readonly servicio = inject(ProyectosService);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.errorCarga = '';
    this.servicio.listar(this.filtro).subscribe({
      next: (lista) => {
        this.proyectos = lista;
        this.cargando = false;
      },
      error: (e: ApiError) => {
        this.cargando = false;
        this.proyectos = [];
        this.errorCarga = e.mensaje;
      },
    });
  }

  filtrar(valor: string): void {
    this.filtro = valor as Estado | '';
    this.limpiarMensajes();
    this.cargar();
  }

  alCrear(p: Proyecto): void {
    this.limpiarMensajes();
    this.mensajeOk = `Proyecto «${p.titulo}» registrado con id ${p.id}.`;
    this.ultimoCreadoId = p.id;
    this.cargar();
  }

  avanzar(p: Proyecto, destino: Estado): void {
    this.limpiarMensajes();
    this.procesandoId = p.id;
    this.servicio.cambiarEstado(p.id, destino).subscribe({
      next: (act) => {
        this.procesandoId = null;
        this.mensajeOk = `«${act.titulo}» ahora está en ${act.estado}.`;
        this.cargar();
      },
      error: (e: ApiError) => {
        this.procesandoId = null;
        this.mensajeError = e.mensaje;
        this.cargar(); // el estado pudo haber cambiado desde otro cliente
      },
    });
  }

  etiqueta(e: Estado): string {
    return e.replace(/_/g, ' ');
  }

  private limpiarMensajes(): void {
    this.mensajeOk = '';
    this.mensajeError = '';
    this.ultimoCreadoId = null;
  }
}
