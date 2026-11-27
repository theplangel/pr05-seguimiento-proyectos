import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthService } from '../core/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let http: HttpTestingController;
  let auth: AuthService;
  const q = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const escribe = (id: string, valor: string) => {
    const input = q(id) as HTMLInputElement;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  };
  const enviar = () => (q('form-login') as HTMLFormElement).dispatchEvent(new Event('submit'));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(LoginComponent);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    fixture.detectChanges();
  });
  afterEach(() => http.verify());

  it('el campo de contraseña es de tipo password', () => {
    expect((q('clave') as HTMLInputElement).type).toBe('password');
  });

  it('con campos vacíos no llama a la API y muestra los errores', () => {
    enviar();
    fixture.detectChanges();
    http.expectNone('/api/auth/yo');
    expect(fixture.nativeElement.textContent).toContain('El usuario es obligatorio');
    expect(fixture.nativeElement.textContent).toContain('La contraseña es obligatoria');
  });

  it('credenciales correctas: abre la sesión y limpia el formulario', () => {
    escribe('usuario', ' lider ');
    escribe('clave', 'clave');
    enviar();
    const req = http.expectOne('/api/auth/yo');
    expect(req.request.headers.get('Authorization')).toBe('Basic bGlkZXI6Y2xhdmU='); // usuario sin espacios
    req.flush({ usuario: 'lider', rol: 'LIDER' });
    fixture.detectChanges();
    expect(auth.sesion?.rol).toBe('LIDER');
    expect((q('clave') as HTMLInputElement).value).toBe('');
  });

  it('credenciales incorrectas (401): mensaje genérico y contraseña borrada', () => {
    escribe('usuario', 'lider');
    escribe('clave', 'mala');
    enviar();
    http.expectOne('/api/auth/yo').flush({ status: 401, mensaje: 'x' }, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();
    expect(q('error-login')?.textContent).toContain('Usuario o contraseña incorrectos');
    expect((q('clave') as HTMLInputElement).value).toBe('');
    expect(auth.sesion).toBeNull();
  });

  it('demasiados intentos (429): avisa que espere', () => {
    escribe('usuario', 'lider');
    escribe('clave', 'mala');
    enviar();
    http.expectOne('/api/auth/yo').flush({ status: 429, mensaje: 'x' }, { status: 429, statusText: 'Too Many Requests' });
    fixture.detectChanges();
    expect(q('error-login')?.textContent).toContain('Demasiados intentos');
  });
});
