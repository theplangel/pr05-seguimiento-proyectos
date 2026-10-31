import { Component, EventEmitter, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError, Proyecto, ProyectoRequest } from '../core/models';
import { ProyectosService } from '../core/proyectos.service';

type Campo = keyof ProyectoRequest;

/** Formulario de alta: valida en cliente lo mismo que el backend (@NotBlank + @Size) y muestra los errores de la API. */
@Component({
  selector: 'app-proyecto-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './proyecto-form.component.html',
})
export class ProyectoFormComponent {
  @Output() readonly creado = new EventEmitter<Proyecto>();

  enviando = false;
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

  invalido(campo: Campo): boolean {
    const c = this.form.controls[campo];
    return c.invalid && (c.dirty || c.touched);
  }

  /** El error del servidor deja de mostrarse en cuanto el usuario corrige el campo. */
  limpiarServidor(campo: Campo): void {
    if (this.erroresServidor[campo]) {
      const resto = { ...this.erroresServidor };
      delete resto[campo];
      this.erroresServidor = resto;
    }
  }

  enviar(): void {
    this.mensajeError = '';
    this.erroresServidor = {};
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando = true;
    this.form.disable();
    this.servicio.crear(this.form.getRawValue()).subscribe({
      next: (p) => {
        this.enviando = false;
        this.form.enable();
        this.form.reset();
        this.creado.emit(p);
      },
      error: (e: ApiError) => {
        this.enviando = false;
        this.form.enable();
        this.mensajeError = e.mensaje;
        this.erroresServidor = e.campos ?? {};
      },
    });
  }
}

/** Rechaza valores que contienen solo espacios (el backend aplica @NotBlank). */
function sinEspacios(control: { value: unknown }) {
  return typeof control.value === 'string' && control.value.length > 0 && control.value.trim().length === 0
    ? { espacios: true }
    : null;
}
