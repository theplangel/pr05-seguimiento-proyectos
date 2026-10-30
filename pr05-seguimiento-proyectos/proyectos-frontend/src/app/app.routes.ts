import { Routes } from '@angular/router';
import { ProyectoDetalleComponent } from './proyectos/proyecto-detalle.component';
import { ProyectosComponent } from './proyectos/proyectos.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'proyectos' },
  { path: 'proyectos', component: ProyectosComponent, title: 'Proyectos · Seguimiento' },
  { path: 'proyectos/:id', component: ProyectoDetalleComponent, title: 'Detalle de proyecto · Seguimiento' },
  { path: '**', redirectTo: 'proyectos' },
];
