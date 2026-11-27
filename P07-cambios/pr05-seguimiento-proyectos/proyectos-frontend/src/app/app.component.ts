import { Component, OnInit, inject } from '@angular/core';
import { LoginComponent } from './auth/login.component';
import { AuthService } from './core/auth.service';
import { ProyectosService } from './core/proyectos.service';
import { DashboardIotComponent } from './iot/dashboard-iot.component';
import { ProyectosComponent } from './proyectos/proyectos.component';

type Vista = 'proyectos' | 'iot';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [ProyectosComponent, DashboardIotComponent, LoginComponent],
  template: `
    <header class="barra">
      <h1>Seguimiento de proyectos estudiantiles</h1>
      <span class="salud" [class.ok]="api === 'UP'" [class.caida]="api === 'DOWN'" data-testid="salud">
        API: {{ api === 'UP' ? 'activa' : api === 'DOWN' ? 'sin conexión' : 'verificando…' }}
      </span>
      @if (auth.sesion; as s) {
        <span class="usuario" data-testid="usuario-actual">{{ s.usuario }} ({{ s.rol }})</span>
        <button type="button" (click)="salir()" data-testid="salir">Cerrar sesión</button>
      }
    </header>
    @if (auth.sesion) {
      <nav class="tabs" role="tablist" aria-label="Secciones">
        <button type="button" role="tab" [attr.aria-selected]="vista === 'proyectos'" (click)="cambiar('proyectos')" data-testid="tab-proyectos">Proyectos</button>
        <button type="button" role="tab" [attr.aria-selected]="vista === 'iot'" (click)="cambiar('iot')" data-testid="tab-iot">Dashboard IoT</button>
      </nav>
      <main>
        @if (vista === 'proyectos') { <app-proyectos /> } @else { <app-dashboard-iot /> }
      </main>
    } @else {
      <main><app-login /></main>
    }
  `,
})
export class AppComponent implements OnInit {
  api: 'UP' | 'DOWN' | '' = '';
  /** Tras iniciar sesión se puede abrir directo en el dashboard con /#iot */
  vista: Vista = typeof location !== 'undefined' && location.hash === '#iot' ? 'iot' : 'proyectos';

  readonly auth = inject(AuthService);
  private readonly servicio = inject(ProyectosService);

  ngOnInit(): void {
    this.servicio.salud().subscribe({
      next: (s) => (this.api = s.status === 'UP' ? 'UP' : 'DOWN'),
      error: () => (this.api = 'DOWN'),
    });
  }

  cambiar(v: Vista): void {
    this.vista = v;
    if (typeof location !== 'undefined') {
      location.hash = v === 'iot' ? 'iot' : '';
    }
  }

  salir(): void {
    this.auth.cerrarSesion();
  }
}
