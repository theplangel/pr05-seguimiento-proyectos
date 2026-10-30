import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  function crear() {
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const fixture = TestBed.createComponent(AppComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    return { fixture, http };
  }
  const salud = (f: ReturnType<typeof crear>['fixture']) =>
    f.nativeElement.querySelector('[data-testid="salud"]').textContent as string;

  it('muestra "verificando…" mientras /api/health no responde', () => {
    const { fixture, http } = crear();
    expect(salud(fixture)).toContain('verificando');
    http.expectOne('/api/health').flush({ status: 'UP', servicio: 'proyectos-api', timestamp: 'x' });
  });

  it('indica "activa" cuando /api/health responde UP', () => {
    const { fixture, http } = crear();
    http.expectOne('/api/health').flush({ status: 'UP', servicio: 'proyectos-api', timestamp: 'x' });
    fixture.detectChanges();
    expect(salud(fixture)).toContain('activa');
    expect(fixture.nativeElement.querySelector('[data-testid="reintentar-salud"]')).toBeNull();
  });

  it('indica "sin conexión" y permite reintentar cuando la API no responde', () => {
    const { fixture, http } = crear();
    http.expectOne('/api/health').error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(salud(fixture)).toContain('sin conexión');

    fixture.nativeElement.querySelector('[data-testid="reintentar-salud"]').click();
    http.expectOne('/api/health').flush({ status: 'UP', servicio: 'proyectos-api', timestamp: 'x' });
    fixture.detectChanges();
    expect(salud(fixture)).toContain('activa');
  });
});
