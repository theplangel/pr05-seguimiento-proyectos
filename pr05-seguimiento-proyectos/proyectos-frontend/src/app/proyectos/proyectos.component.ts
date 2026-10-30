import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError, ESTADOS, Estado, Proyecto } from '../core/models';
import { ProyectosService } from '../core/proyectos.service';

@Component({
  selector: 'app-proyectos',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './proyectos.component.html',
})
export class ProyectosComponent implements OnInit {
  readonly estados = ESTADOS;
  proyectos: Proyecto[] = [];
  filtro: Estado | '' = '';
  cargando = false;
  enviando = false;
  mensajeOk = '';
  mensajeError = '';
  erroresServidor: Record<string, string> = {};

  private readonly fb = inject(FormBuilder);
  private readonly servicio = inject(ProyectosService);

  readonly form = this.fb.nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(120), sinEspacios]],
    descripcion: ['', [Validators.required, Validators.maxLength(500), sinEspacios]],
    integrantes: ['', [Validators.required, Validators.maxLength(250), sinEspacios]],
    asesor: ['', [Validators.required, Validators.maxLength(120), sinEspacios]],
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.servicio.listar(this.filtro).subscribe({
      next: (lista) => {
        this.proyectos = lista;
        this.cargando = false;
      },
      error: (e: ApiError) => {
        this.cargando = false;
        this.mensajeError = e.mensaje;
      },
    });
  }

  filtrar(valor: string): void {
    this.filtro = valor as Estado | '';
    this.limpiarMensajes();
    this.cargar();
  }

  invalido(campo: 'titulo' | 'descripcion' | 'integrantes' | 'asesor'): boolean {
    const c = this.form.controls[campo];
    return c.invalid && (c.dirty || c.touched);
  }

  crear(): void {
    this.limpiarMensajes();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando = true;
    this.servicio.crear(this.form.getRawValue()).subscribe({
      next: (p) => {
        this.enviando = false;
        this.mensajeOk = `Proyecto «${p.titulo}» registrado con id ${p.id}.`;
        this.form.reset();
        this.cargar();
      },
      error: (e: ApiError) => {
        this.enviando = false;
        this.mensajeError = e.mensaje;
        this.erroresServidor = e.campos ?? {};
      },
    });
  }

  avanzar(p: Proyecto, destino: Estado): void {
    this.limpiarMensajes();
    this.servicio.cambiarEstado(p.id, destino).subscribe({
      next: (act) => {
        this.mensajeOk = `«${act.titulo}» ahora está en ${act.estado}.`;
        this.cargar();
      },
      error: (e: ApiError) => {
        this.mensajeError = e.mensaje;
        this.cargar(); // el estado pudo haber cambiado desde otro cliente
      },
    });
  }

  private limpiarMensajes(): void {
    this.mensajeOk = '';
    this.mensajeError = '';
    this.erroresServidor = {};
  }
}

/** Rechaza valores que contienen solo espacios (el backend aplica @NotBlank). */
function sinEspacios(control: { value: unknown }) {
  return typeof control.value === 'string' && control.value.length > 0 && control.value.trim().length === 0
    ? { espacios: true }
    : null;
}
