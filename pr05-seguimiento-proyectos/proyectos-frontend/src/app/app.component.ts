import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { LoginComponent } from './auth/login.component';
import { AuthService } from './core/auth.service';
import { ProyectosService } from './core/proyectos.service';

type Vista = 'proyectos' | 'iot';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [LoginComponent, RouterLink, RouterOutlet],
  template: `
    <header class="barra">
      <h1><a routerLink="/proyectos">Seguimiento de proyectos estudiantiles</a></h1>
      <span class="salud" [class.ok]="api === 'UP'" [class.caida]="api === 'DOWN'" data-testid="salud">
        API: {{ api === 'UP' ? 'activa' : api === 'DOWN' ? 'sin conexión' : 'verificando…' }}
      </span>
      @if (auth.sesion; as s) {
        <span class="usuario" data-testid="usuario-actual">{{ s.usuario }} ({{ s.rol }})</span>
        <button type="button" (click)="salir()" data-testid="salir">Cerrar sesión</button>
      }
      @if (api === 'DOWN') {
        <button type="button" class="secundario" (click)="verificar()" data-testid="reintentar-salud">Reintentar</button>
      }
    </header>
    @if (auth.sesion) {
      <nav class="navegacion tabs" role="tablist" aria-label="Secciones">
        <a role="tab" [attr.aria-selected]="vista === 'proyectos'" routerLink="/proyectos" data-testid="tab-proyectos">Proyectos</a>
        <a role="tab" [attr.aria-selected]="vista === 'iot'" routerLink="/iot" data-testid="tab-iot">Dashboard IoT</a>
      </nav>
      <main><router-outlet /></main>
    } @else {
      <main><app-login /></main>
    }
  `,
})
export class AppComponent implements OnInit {
  api: 'UP' | 'DOWN' | '' = '';

  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly servicio = inject(ProyectosService);

  get vista(): Vista {
    return this.router.url.startsWith('/iot') ? 'iot' : 'proyectos';
  }

  ngOnInit(): void {
    if (typeof location !== 'undefined' && location.hash === '#iot') {
      location.hash = '#/iot';
    }
    this.verificar();
  }

  verificar(): void {
    this.api = '';
    this.servicio.salud().subscribe({
      next: (s) => (this.api = s.status === 'UP' ? 'UP' : 'DOWN'),
      error: () => (this.api = 'DOWN'),
    });
  }

  salir(): void {
    this.auth.cerrarSesion();
    this.router.navigateByUrl('/proyectos');
  }
}
