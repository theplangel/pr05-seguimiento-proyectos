import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Proyecto } from '../core/models';
import { ProyectosComponent } from './proyectos.component';

const p = (o: Partial<Proyecto> = {}): Proyecto => ({
  id: 1, titulo: 'Tutorías', descripcion: 'D', integrantes: 'Ana', asesor: 'Dr. X',
  estado: 'REGISTRADO', estadosPermitidos: ['EN_PROGRESO'],
  fechaCreacion: '2026-10-01T00:00:00', fechaActualizacion: '2026-10-01T00:00:00', ...o,
});

describe('ProyectosComponent (flujo de usuario Angular → API)', () => {
  let fixture: ComponentFixture<ProyectosComponent>;
  let http: HttpTestingController;
  const el = () => fixture.nativeElement as HTMLElement;
  const q = (id: string) => el().querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const escribe = (id: string, valor: string) => {
    const input = q(id) as HTMLInputElement;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  };
  const llena = (titulo = 'Nuevo') => {
    escribe('titulo', titulo); escribe('descripcion', 'Desc'); escribe('integrantes', 'Ana'); escribe('asesor', 'Dr. X');
  };
  const envia = () => (q('form-proyecto') as HTMLFormElement).dispatchEvent(new Event('submit'));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ProyectosComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(ProyectosComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges(); // ngOnInit -> GET /api/proyectos
    expect(q('cargando')).not.toBeNull(); // filas esqueleto mientras llega la respuesta
    http.expectOne('/api/proyectos').flush([p()]);
    fixture.detectChanges();
  });
  afterEach(() => http.verify());

  it('muestra en la tabla los proyectos devueltos por la API', () => {
    expect(q('cargando')).toBeNull();
    expect(el().querySelectorAll('[data-testid="fila"]').length).toBe(1);
    expect(el().textContent).toContain('Tutorías');
  });

  it('cada título enlaza a su detalle /proyectos/{id}', () => {
    expect(q('detalle-1')?.getAttribute('href')).toBe('/proyectos/1');
  });

  it('flujo completo: registrar proyecto → POST → recarga el listado', () => {
    llena();
    envia();
    const post = http.expectOne((r) => r.method === 'POST');
    expect(post.request.url).toBe('/api/proyectos');
    expect(post.request.body).toEqual({ titulo: 'Nuevo', descripcion: 'Desc', integrantes: 'Ana', asesor: 'Dr. X' });
    fixture.detectChanges();
    expect(q('guardar')?.textContent).toContain('Guardando');
    expect((q('guardar') as HTMLButtonElement).disabled).toBe(true);
    post.flush(p({ id: 2, titulo: 'Nuevo' }), { status: 201, statusText: 'Created' });
    http.expectOne('/api/proyectos').flush([p(), p({ id: 2, titulo: 'Nuevo' })]);
    fixture.detectChanges();
    expect(q('ok')?.textContent).toContain('registrado con id 2');
    expect(q('ver-creado')?.getAttribute('href')).toBe('/proyectos/2');
    expect(el().querySelectorAll('[data-testid="fila"]').length).toBe(2);
    expect((q('titulo') as HTMLInputElement).value).toBe('');
  });

  it('validación en cliente: formulario vacío NO llama a la API', () => {
    envia();
    fixture.detectChanges();
    http.expectNone((r) => r.method === 'POST');
    expect(el().querySelectorAll('.err').length).toBe(4);
  });

  it('un título de solo espacios se rechaza en cliente', () => {
    llena('     ');
    envia();
    fixture.detectChanges();
    http.expectNone((r) => r.method === 'POST');
  });

  it('muestra el error 409 de título duplicado devuelto por la API', () => {
    llena('Tutorías');
    envia();
    http.expectOne((r) => r.method === 'POST').flush(
      { status: 409, mensaje: "Ya existe un proyecto con el título 'Tutorías'" },
      { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(q('error-form')?.textContent).toContain('Ya existe un proyecto');
    expect((q('titulo') as HTMLInputElement).value).toBe('Tutorías'); // no se pierde lo capturado
  });

  it('muestra errores por campo cuando la API responde 400 y los limpia al corregir', () => {
    llena();
    envia();
    http.expectOne((r) => r.method === 'POST').flush(
      { status: 400, mensaje: 'Datos de entrada inválidos', campos: { asesor: 'El asesor es obligatorio' } },
      { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    expect(q('error-form')?.textContent).toContain('Datos de entrada inválidos');
    expect(el().textContent).toContain('El asesor es obligatorio');

    escribe('asesor', 'Dra. Y');
    fixture.detectChanges();
    expect(el().textContent).not.toContain('El asesor es obligatorio');
  });

  it('cambio de estado: botón → PATCH → recarga con el nuevo estado', () => {
    (q('mover-EN_PROGRESO') as HTMLButtonElement).click();
    const patch = http.expectOne('/api/proyectos/1/estado');
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ estado: 'EN_PROGRESO' });
    patch.flush(p({ estado: 'EN_PROGRESO', estadosPermitidos: ['EN_REVISION'] }));
    http.expectOne('/api/proyectos').flush([p({ estado: 'EN_PROGRESO', estadosPermitidos: ['EN_REVISION'] })]);
    fixture.detectChanges();
    expect(q('ok')?.textContent).toContain('EN_PROGRESO');
    expect(q('mover-EN_REVISION')).not.toBeNull();
  });

  it('mientras el PATCH está en curso la fila muestra "Actualizando…" y bloquea las demás acciones', () => {
    const select = q('filtro') as HTMLSelectElement;
    select.value = '';
    select.dispatchEvent(new Event('change'));
    http.expectOne('/api/proyectos').flush([p(), p({ id: 2, titulo: 'Otro' })]);
    fixture.detectChanges();

    (el().querySelectorAll('[data-testid="mover-EN_PROGRESO"]')[0] as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(q('procesando')?.textContent).toContain('Actualizando');
    const restantes = el().querySelectorAll<HTMLButtonElement>('[data-testid="mover-EN_PROGRESO"]');
    expect(restantes.length).toBe(1);
    expect(restantes[0].disabled).toBe(true);

    http.expectOne('/api/proyectos/1/estado').flush(p({ estado: 'EN_PROGRESO', estadosPermitidos: ['EN_REVISION'] }));
    http.expectOne('/api/proyectos').flush([]);
  });

  it('transición rechazada (409) muestra el mensaje y refresca el listado', () => {
    (q('mover-EN_PROGRESO') as HTMLButtonElement).click();
    http.expectOne('/api/proyectos/1/estado').flush(
      { status: 409, mensaje: 'Transición no permitida: REGISTRADO -> EN_PROGRESO' },
      { status: 409, statusText: 'Conflict' });
    http.expectOne('/api/proyectos').flush([p()]);
    fixture.detectChanges();
    expect(q('error')?.textContent).toContain('Transición no permitida');
  });

  it('si la API no responde muestra el error de carga y "Reintentar" vuelve a consultar', () => {
    const select = q('filtro') as HTMLSelectElement;
    select.value = '';
    select.dispatchEvent(new Event('change'));
    http.expectOne('/api/proyectos').error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(q('error-carga')?.textContent).toContain('No se pudo conectar');
    expect(q('tabla')).toBeNull();
    expect(q('vacio')).toBeNull();

    (q('reintentar') as HTMLButtonElement).click();
    http.expectOne('/api/proyectos').flush([p()]);
    fixture.detectChanges();
    expect(q('error-carga')).toBeNull();
    expect(el().querySelectorAll('[data-testid="fila"]').length).toBe(1);
  });

  it('filtrar por estado consulta /api/proyectos?estado=…', () => {
    const select = q('filtro') as HTMLSelectElement;
    select.value = 'ENTREGADO';
    select.dispatchEvent(new Event('change'));
    const req = http.expectOne((r) => r.url === '/api/proyectos');
    expect(req.request.params.get('estado')).toBe('ENTREGADO');
    req.flush([]);
    fixture.detectChanges();
    expect(q('vacio')).not.toBeNull();
  });

  it('un proyecto ENTREGADO no ofrece acciones', () => {
    const select = q('filtro') as HTMLSelectElement;
    select.value = '';
    select.dispatchEvent(new Event('change'));
    http.expectOne('/api/proyectos').flush([p({ estado: 'ENTREGADO', estadosPermitidos: [] })]);
    fixture.detectChanges();
    expect(el().textContent).toContain('Estado final');
  });
});
