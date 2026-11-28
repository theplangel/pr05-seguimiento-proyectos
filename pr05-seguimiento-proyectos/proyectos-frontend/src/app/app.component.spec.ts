import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { authInterceptor } from './core/auth.interceptor';

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let http: HttpTestingController;
  const q = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;

  function crear() {
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(AppComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  function iniciarSesion(usuario = 'lider', rol = 'LIDER') {
    const poner = (id: string, valor: string) => {
      const i = q(id) as HTMLInputElement;
      i.value = valor;
      i.dispatchEvent(new Event('input'));
    };
    poner('usuario', usuario);
    poner('clave', 'clave');
    (q('form-login') as HTMLFormElement).dispatchEvent(new Event('submit'));
    http.expectOne('/api/auth/yo').flush({ usuario, rol });
    fixture.detectChanges();
  }

  it('sin sesión muestra el login y NO consulta proyectos', () => {
    crear();
    http.expectOne('/api/health').flush({ status: 'UP', servicio: 'x', timestamp: 'x' });
    http.expectNone('/api/proyectos');
    expect(q('form-login')).not.toBeNull();
    expect(q('tab-iot')).toBeNull();
    http.verify();
  });

  it('tras iniciar sesión carga los proyectos enviando la credencial y muestra usuario y rol', () => {
    crear();
    http.expectOne('/api/health').flush({ status: 'UP', servicio: 'x', timestamp: 'x' });
    iniciarSesion();
    const req = http.expectOne('/api/proyectos');
    expect(req.request.headers.get('Authorization')).toMatch(/^Basic /);
    req.flush([]);
    fixture.detectChanges();
    expect(q('usuario-actual')?.textContent).toContain('lider (LIDER)');
    expect(q('tab-iot')).not.toBeNull();
    http.verify();
  });

  it('cerrar sesión vuelve al login', () => {
    crear();
    http.expectOne('/api/health').flush({ status: 'UP', servicio: 'x', timestamp: 'x' });
    iniciarSesion();
    http.expectOne('/api/proyectos').flush([]);
    fixture.detectChanges();
    (q('salir') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(q('form-login')).not.toBeNull();
    expect(q('usuario-actual')).toBeNull();
    http.verify();
  });

  it('indica "activa" cuando /api/health responde UP', () => {
    crear();
    http.expectOne('/api/health').flush({ status: 'UP', servicio: 'proyectos-api', timestamp: 'x' });
    fixture.detectChanges();
    expect(q('salud')?.textContent).toContain('activa');
  });

  it('indica "sin conexión" cuando la API no responde', () => {
    crear();
    http.expectOne('/api/health').error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(q('salud')?.textContent).toContain('sin conexión');
  });
});
