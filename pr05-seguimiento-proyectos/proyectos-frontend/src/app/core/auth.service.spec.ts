import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { ApiError } from './models';

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('sin iniciar sesión no hay sesión ni credencial', () => {
    expect(auth.sesion).toBeNull();
    expect(auth.autorizacion).toBeNull();
    expect(auth.esLider()).toBe(false);
  });

  it('login correcto: envía Basic a /api/auth/yo y guarda usuario y rol', () => {
    let sesion: unknown;
    auth.login('lider', 'clave').subscribe((s) => (sesion = s));
    const req = http.expectOne('/api/auth/yo');
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.get('Authorization')).toBe('Basic bGlkZXI6Y2xhdmU=');
    req.flush({ usuario: 'lider', rol: 'LIDER' });
    expect(sesion).toEqual({ usuario: 'lider', rol: 'LIDER' });
    expect(auth.sesion).toEqual({ usuario: 'lider', rol: 'LIDER' });
    expect(auth.autorizacion).toBe('Basic bGlkZXI6Y2xhdmU=');
    expect(auth.esLider()).toBe(true);
  });

  it('un INTEGRANTE no es líder', () => {
    auth.login('integrante', 'clave').subscribe();
    http.expectOne('/api/auth/yo').flush({ usuario: 'integrante', rol: 'INTEGRANTE' });
    expect(auth.esLider()).toBe(false);
  });

  it('login incorrecto (401): no guarda nada y devuelve un ApiError', () => {
    let error: ApiError | undefined;
    auth.login('lider', 'mala').subscribe({ error: (e) => (error = e) });
    http.expectOne('/api/auth/yo').flush(
      { status: 401, mensaje: 'Autenticación requerida o credenciales inválidas' },
      { status: 401, statusText: 'Unauthorized' });
    expect(error?.status).toBe(401);
    expect(auth.sesion).toBeNull();
    expect(auth.autorizacion).toBeNull();
  });

  it('codifica en UTF-8 los caracteres no ASCII de la contraseña', () => {
    auth.login('añ', 'ñ').subscribe();
    const req = http.expectOne('/api/auth/yo');
    expect(req.request.headers.get('Authorization')).toBe('Basic YcOxOsOx');
    req.flush({ usuario: 'añ', rol: 'INTEGRANTE' });
  });

  it('cerrar sesión borra la credencial de memoria', () => {
    auth.login('lider', 'clave').subscribe();
    http.expectOne('/api/auth/yo').flush({ usuario: 'lider', rol: 'LIDER' });
    auth.cerrarSesion();
    expect(auth.sesion).toBeNull();
    expect(auth.autorizacion).toBeNull();
  });

  it('la credencial no se guarda en localStorage ni sessionStorage', () => {
    auth.login('lider', 'clave').subscribe();
    http.expectOne('/api/auth/yo').flush({ usuario: 'lider', rol: 'LIDER' });
    expect(JSON.stringify({ ...localStorage })).not.toContain('bGlkZXI6');
    expect(JSON.stringify({ ...sessionStorage })).not.toContain('bGlkZXI6');
  });
});
