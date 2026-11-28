import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiError, Proyecto } from './models';
import { ProyectosService } from './proyectos.service';

const proyecto = (o: Partial<Proyecto> = {}): Proyecto => ({
  id: 1, titulo: 'T', descripcion: 'D', integrantes: 'A', asesor: 'X',
  estado: 'REGISTRADO', estadosPermitidos: ['EN_PROGRESO'],
  fechaCreacion: '2026-10-01T00:00:00', fechaActualizacion: '2026-10-01T00:00:00', ...o,
});

describe('ProyectosService (contrato con la API)', () => {
  let servicio: ProyectosService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(ProyectosService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('GET /api/health devuelve el estado del servicio', () => {
    let status = '';
    servicio.salud().subscribe((s) => (status = s.status));
    http.expectOne('/api/health').flush({ status: 'UP', servicio: 'proyectos-api', timestamp: 'x' });
    expect(status).toBe('UP');
  });

  it('GET /api/proyectos sin filtro no envía parámetros', () => {
    servicio.listar().subscribe();
    const req = http.expectOne('/api/proyectos');
    expect(req.request.params.keys()).toEqual([]);
    req.flush([]);
  });

  it('GET /api/proyectos?estado=EN_REVISION envía el filtro', () => {
    servicio.listar('EN_REVISION').subscribe();
    const req = http.expectOne((r) => r.url === '/api/proyectos');
    expect(req.request.params.get('estado')).toBe('EN_REVISION');
    req.flush([]);
  });

  it('POST /api/proyectos envía el cuerpo y devuelve el recurso creado', () => {
    const datos = { titulo: 'T', descripcion: 'D', integrantes: 'A', asesor: 'X' };
    let creado: Proyecto | undefined;
    servicio.crear(datos).subscribe((p) => (creado = p));
    const req = http.expectOne('/api/proyectos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(datos);
    req.flush(proyecto(), { status: 201, statusText: 'Created' });
    expect(creado?.estado).toBe('REGISTRADO');
  });

  it('PATCH /api/proyectos/{id}/estado envía solo el estado destino', () => {
    servicio.cambiarEstado(7, 'EN_PROGRESO').subscribe();
    const req = http.expectOne('/api/proyectos/7/estado');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ estado: 'EN_PROGRESO' });
    req.flush(proyecto({ estado: 'EN_PROGRESO' }));
  });

  it('traduce un 400 con campos al modelo ApiError', () => {
    let error: ApiError | undefined;
    servicio.crear({ titulo: '', descripcion: '', integrantes: '', asesor: '' }).subscribe({ error: (e) => (error = e) });
    http.expectOne('/api/proyectos').flush(
      { status: 400, mensaje: 'Datos de entrada inválidos', campos: { titulo: 'El título es obligatorio' } },
      { status: 400, statusText: 'Bad Request' });
    expect(error?.status).toBe(400);
    expect(error?.campos?.['titulo']).toBe('El título es obligatorio');
  });

  it('traduce un 409 de transición inválida', () => {
    let error: ApiError | undefined;
    servicio.cambiarEstado(1, 'ENTREGADO').subscribe({ error: (e) => (error = e) });
    http.expectOne('/api/proyectos/1/estado').flush(
      { status: 409, mensaje: 'Transición no permitida: REGISTRADO -> ENTREGADO' },
      { status: 409, statusText: 'Conflict' });
    expect(error?.status).toBe(409);
    expect(error?.mensaje).toContain('REGISTRADO -> ENTREGADO');
  });

  it('traduce un 404', () => {
    let error: ApiError | undefined;
    servicio.obtener(99).subscribe({ error: (e) => (error = e) });
    http.expectOne('/api/proyectos/99').flush({ status: 404, mensaje: 'No existe el proyecto con id 99' },
      { status: 404, statusText: 'Not Found' });
    expect(error?.status).toBe(404);
  });

  it('informa cuando no hay conexión con la API (status 0)', () => {
    let error: ApiError | undefined;
    servicio.listar().subscribe({ error: (e) => (error = e) });
    http.expectOne('/api/proyectos').error(new ProgressEvent('error'));
    expect(error?.status).toBe(0);
    expect(error?.mensaje).toContain('No se pudo conectar');
  });
});
