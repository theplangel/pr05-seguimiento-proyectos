import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Proyecto } from '../core/models';
import { ProyectoDetalleComponent } from './proyecto-detalle.component';

const p = (o: Partial<Proyecto> = {}): Proyecto => ({
  id: 3, titulo: 'Bitácora', descripcion: 'Registro de avances', integrantes: 'Ana, Luis', asesor: 'Dra. Y',
  estado: 'EN_PROGRESO', estadosPermitidos: ['EN_REVISION'],
  fechaCreacion: '2026-10-01T09:30:00', fechaActualizacion: '2026-10-02T10:15:00', ...o,
});

describe('ProyectoDetalleComponent (GET /api/proyectos/{id} y cambio de estado)', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;
  const el = () => harness.routeNativeElement as HTMLElement;
  const q = (id: string) => el().querySelector(`[data-testid="${id}"]`) as HTMLElement | null;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'proyectos/:id', component: ProyectoDetalleComponent }]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });
  afterEach(() => http.verify());

  async function abrir(url = '/proyectos/3') {
    await harness.navigateByUrl(url, ProyectoDetalleComponent);
  }

  it('muestra el indicador de carga y luego todos los datos del proyecto', async () => {
    await abrir();
    expect(q('cargando')).not.toBeNull();
    http.expectOne('/api/proyectos/3').flush(p());
    harness.detectChanges();
    expect(q('cargando')).toBeNull();
    expect(q('titulo-detalle')?.textContent).toContain('#3 · Bitácora');
    expect(q('estado-actual')?.textContent).toContain('EN PROGRESO');
    expect(el().textContent).toContain('Ana, Luis');
    expect(el().textContent).toContain('Dra. Y');
    expect(q('actualizado')?.textContent).toContain('02/10/2026 10:15');
    expect(el().querySelector('.ciclo li.actual')?.textContent).toContain('EN PROGRESO');
    expect(el().querySelectorAll('.ciclo li.hecho').length).toBe(1);
  });

  it('cambio de estado desde el detalle: PATCH y refleja la respuesta sin otro GET', async () => {
    await abrir();
    http.expectOne('/api/proyectos/3').flush(p());
    harness.detectChanges();

    (q('mover-EN_REVISION') as HTMLButtonElement).click();
    harness.detectChanges();
    expect(q('procesando')).not.toBeNull();
    expect(q('mover-EN_REVISION')).toBeNull();

    const patch = http.expectOne('/api/proyectos/3/estado');
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ estado: 'EN_REVISION' });
    patch.flush(p({ estado: 'EN_REVISION', estadosPermitidos: ['EN_PROGRESO', 'ENTREGADO'] }));
    harness.detectChanges();

    expect(q('ok')?.textContent).toContain('EN REVISION');
    expect(q('estado-actual')?.textContent).toContain('EN REVISION');
    expect(q('mover-ENTREGADO')).not.toBeNull();
    expect(q('mover-EN_PROGRESO')).not.toBeNull();
  });

  it('409 al cambiar estado: muestra el mensaje de la API y recarga el recurso', async () => {
    await abrir();
    http.expectOne('/api/proyectos/3').flush(p());
    harness.detectChanges();

    (q('mover-EN_REVISION') as HTMLButtonElement).click();
    http.expectOne('/api/proyectos/3/estado').flush(
      { status: 409, mensaje: 'Transición no permitida: ENTREGADO -> EN_REVISION' },
      { status: 409, statusText: 'Conflict' });
    http.expectOne('/api/proyectos/3').flush(p({ estado: 'ENTREGADO', estadosPermitidos: [] }));
    harness.detectChanges();

    expect(q('error')?.textContent).toContain('Transición no permitida');
    expect(q('final')).not.toBeNull();
  });

  it('404: informa que el proyecto no existe, sin botón de reintento', async () => {
    await abrir('/proyectos/999');
    http.expectOne('/api/proyectos/999').flush(
      { status: 404, mensaje: 'No existe el proyecto con id 999' }, { status: 404, statusText: 'Not Found' });
    harness.detectChanges();
    expect(q('error-detalle')?.textContent).toContain('Proyecto no encontrado');
    expect(q('error-detalle')?.textContent).toContain('No existe el proyecto con id 999');
    expect(q('reintentar')).toBeNull();
    expect(q('volver')?.getAttribute('href')).toBe('/proyectos');
  });

  it('id no numérico: se rechaza en cliente sin llamar a la API', async () => {
    await abrir('/proyectos/abc');
    http.expectNone((r) => r.url.startsWith('/api/'));
    expect(q('error-detalle')?.textContent).toContain('no es un identificador');
  });

  it('sin conexión: ofrece "Reintentar" y vuelve a pedir el recurso', async () => {
    await abrir();
    http.expectOne('/api/proyectos/3').error(new ProgressEvent('error'));
    harness.detectChanges();
    expect(q('error-detalle')?.textContent).toContain('No se pudo conectar');

    (q('reintentar') as HTMLButtonElement).click();
    http.expectOne('/api/proyectos/3').flush(p());
    harness.detectChanges();
    expect(q('detalle')).not.toBeNull();
  });
});
