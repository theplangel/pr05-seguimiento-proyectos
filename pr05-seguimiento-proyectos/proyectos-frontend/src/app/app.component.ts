import { Component, OnInit } from '@angular/core';
import { ProyectosService } from './core/proyectos.service';
import { ProyectosComponent } from './proyectos/proyectos.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [ProyectosComponent],
  template: `
    <header class="barra">
      <h1>Seguimiento de proyectos estudiantiles</h1>
      <span class="salud" [class.ok]="api === 'UP'" [class.caida]="api === 'DOWN'" data-testid="salud">
        API: {{ api === 'UP' ? 'activa' : api === 'DOWN' ? 'sin conexión' : 'verificando…' }}
      </span>
    </header>
    <main><app-proyectos /></main>
  `,
})
export class AppComponent implements OnInit {
  api: 'UP' | 'DOWN' | '' = '';

  constructor(private readonly servicio: ProyectosService) {}

  ngOnInit(): void {
    this.servicio.salud().subscribe({
      next: (s) => (this.api = s.status === 'UP' ? 'UP' : 'DOWN'),
      error: () => (this.api = 'DOWN'),
    });
  }
}
