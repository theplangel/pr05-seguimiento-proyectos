import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError } from '../core/models';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="panel login">
      <h2>Iniciar sesión</h2>
      <form [formGroup]="form" (ngSubmit)="entrar()" novalidate data-testid="form-login">
        <label>Usuario
          <input formControlName="usuario" maxlength="50" autocomplete="username" data-testid="usuario" />
          @if (invalido('usuario')) { <small class="err">El usuario es obligatorio.</small> }
        </label>
        <label>Contraseña
          <input type="password" formControlName="clave" maxlength="100" autocomplete="current-password" data-testid="clave" />
          @if (invalido('clave')) { <small class="err">La contraseña es obligatoria.</small> }
        </label>
        <button type="submit" [disabled]="enviando" data-testid="entrar">{{ enviando ? 'Verificando…' : 'Entrar' }}</button>
      </form>
      @if (error) { <p class="banner error" role="alert" data-testid="error-login">{{ error }}</p> }
    </section>
  `,
})
export class LoginComponent {
  enviando = false;
  error = '';

  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly form = this.fb.nonNullable.group({
    usuario: ['', [Validators.required, Validators.maxLength(50)]],
    clave: ['', [Validators.required, Validators.maxLength(100)]],
  });

  invalido(campo: 'usuario' | 'clave'): boolean {
    const c = this.form.controls[campo];
    return c.invalid && (c.dirty || c.touched);
  }

  entrar(): void {
    this.error = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando = true;
    const { usuario, clave } = this.form.getRawValue();
    this.auth.login(usuario.trim(), clave).subscribe({
      next: () => {
        this.enviando = false;
        this.form.reset(); // la contraseña no se queda en el formulario
      },
      error: (e: ApiError) => {
        this.enviando = false;
        this.form.controls.clave.reset();
        this.error =
          e.status === 401 ? 'Usuario o contraseña incorrectos.'
          : e.status === 429 ? 'Demasiados intentos fallidos. Espera un minuto e inténtalo de nuevo.'
          : e.mensaje;
      },
    });
  }
}
