import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
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

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ProyectosComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(ProyectosComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges(); // ngOnInit -> GET /api/proyectos
    http.expectOne('/api/proyectos').flush([p()]);
    fixture.detectChanges();
  });
  afterEach(() => http.verify());

  it('muestra en la tabla los proyectos devueltos por la API', () => {
    expect(el().querySelectorAll('[data-testid="fila"]').length).toBe(1);
    expect(el().textContent).toContain('Tutorías');
  });

  it('flujo completo: registrar proyecto → POST → recarga el listado', () => {
    llena();
    (q('form-proyecto') as HTMLFormElement).dispatchEvent(new Event('submit'));
    const post = http.expectOne((r) => r.method === 'POST');
    expect(post.request.url).toBe('/api/proyectos');
    expect(post.request.body).toEqual({ titulo: 'Nuevo', descripcion: 'Desc', integrantes: 'Ana', asesor: 'Dr. X' });
    post.flush(p({ id: 2, titulo: 'Nuevo' }), { status: 201, statusText: 'Created' });
    http.expectOne('/api/proyectos').flush([p(), p({ id: 2, titulo: 'Nuevo' })]);
    fixture.detectChanges();
    expect(q('ok')?.textContent).toContain('registrado con id 2');
    expect(el().querySelectorAll('[data-testid="fila"]').length).toBe(2);
  });

  it('validación en cliente: formulario vacío NO llama a la API', () => {
    (q('form-proyecto') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    http.expectNone((r) => r.method === 'POST');
    expect(el().querySelectorAll('.err').length).toBe(4);
  });

  it('un título de solo espacios se rechaza en cliente', () => {
    llena('     ');
    (q('form-proyecto') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    http.expectNone((r) => r.method === 'POST');
  });

  it('muestra el error 409 de título duplicado devuelto por la API', () => {
    llena('Tutorías');
    (q('form-proyecto') as HTMLFormElement).dispatchEvent(new Event('submit'));
    http.expectOne((r) => r.method === 'POST').flush(
      { status: 409, mensaje: "Ya existe un proyecto con el título 'Tutorías'" },
      { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(q('error')?.textContent).toContain('Ya existe un proyecto');
  });

  it('muestra errores por campo cuando la API responde 400', () => {
    llena();
    (q('form-proyecto') as HTMLFormElement).dispatchEvent(new Event('submit'));
    http.expectOne((r) => r.method === 'POST').flush(
      { status: 400, mensaje: 'Datos de entrada inválidos', campos: { asesor: 'El asesor es obligatorio' } },
      { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    expect(q('error')?.textContent).toContain('Datos de entrada inválidos');
    expect(el().textContent).toContain('El asesor es obligatorio');
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

  it('transición rechazada (409) muestra el mensaje y refresca el listado', () => {
    (q('mover-EN_PROGRESO') as HTMLButtonElement).click();
    http.expectOne('/api/proyectos/1/estado').flush(
      { status: 409, mensaje: 'Transición no permitida: REGISTRADO -> EN_PROGRESO' },
      { status: 409, statusText: 'Conflict' });
    http.expectOne('/api/proyectos').flush([p()]);
    fixture.detectChanges();
    expect(q('error')?.textContent).toContain('Transición no permitida');
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
