import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  function crear() {
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(AppComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne('/api/proyectos').flush([]);
    return { fixture, http };
  }

  it('indica "activa" cuando /api/health responde UP', () => {
    const { fixture, http } = crear();
    http.expectOne('/api/health').flush({ status: 'UP', servicio: 'proyectos-api', timestamp: 'x' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="salud"]').textContent).toContain('activa');
  });

  it('indica "sin conexión" cuando la API no responde', () => {
    const { fixture, http } = crear();
    http.expectOne('/api/health').error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="salud"]').textContent).toContain('sin conexión');
  });
});
