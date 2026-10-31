import { Component, OnInit } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { ProyectosService } from './core/proyectos.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <header class="barra">
      <h1><a routerLink="/proyectos">Seguimiento de proyectos estudiantiles</a></h1>
      <span class="salud" [class.ok]="api === 'UP'" [class.caida]="api === 'DOWN'" data-testid="salud">
        API: {{ api === 'UP' ? 'activa' : api === 'DOWN' ? 'sin conexión' : 'verificando…' }}
      </span>
      @if (api === 'DOWN') {
        <button type="button" class="secundario" (click)="verificar()" data-testid="reintentar-salud">Reintentar</button>
      }
    </header>
    <main><router-outlet /></main>
  `,
})
export class AppComponent implements OnInit {
  api: 'UP' | 'DOWN' | '' = '';

  constructor(private readonly servicio: ProyectosService) {}

  ngOnInit(): void {
    this.verificar();
  }

  verificar(): void {
    this.api = '';
    this.servicio.salud().subscribe({
      next: (s) => (this.api = s.status === 'UP' ? 'UP' : 'DOWN'),
      error: () => (this.api = 'DOWN'),
    });
  }
}
