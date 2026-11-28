import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });
  afterEach(() => ctrl.verify());

  function iniciarSesion() {
    auth.login('lider', 'clave').subscribe();
    ctrl.expectOne('/api/auth/yo').flush({ usuario: 'lider', rol: 'LIDER' });
  }

  it('sin sesión no añade Authorization', () => {
    http.get('/api/proyectos').subscribe();
    expect(ctrl.expectOne('/api/proyectos').request.headers.has('Authorization')).toBe(false);
  });

  it('con sesión añade Authorization a las llamadas a la API', () => {
    iniciarSesion();
    http.get('/api/proyectos').subscribe();
    expect(ctrl.expectOne('/api/proyectos').request.headers.get('Authorization')).toBe('Basic bGlkZXI6Y2xhdmU=');
  });

  it('nunca envía la credencial a otros orígenes', () => {
    iniciarSesion();
    http.get('https://otro-sitio.example/datos').subscribe();
    expect(ctrl.expectOne('https://otro-sitio.example/datos').request.headers.has('Authorization')).toBe(false);
  });

  it('un 401 de la API cierra la sesión', () => {
    iniciarSesion();
    http.get('/api/proyectos').subscribe({ error: () => undefined });
    ctrl.expectOne('/api/proyectos').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.sesion).toBeNull();
  });

  it('un 403 NO cierra la sesión (el usuario sigue autenticado, solo le falta permiso)', () => {
    iniciarSesion();
    http.patch('/api/proyectos/1/estado', {}).subscribe({ error: () => undefined });
    ctrl.expectOne('/api/proyectos/1/estado').flush({}, { status: 403, statusText: 'Forbidden' });
    expect(auth.sesion).not.toBeNull();
  });
});
