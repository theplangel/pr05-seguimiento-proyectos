import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { EstadoIot } from './iot.models';
import { IotService } from './iot.service';
import { ApiError } from './models';

const estado: EstadoIot = {
  dispositivo: 'sensor-lab-01', nombre: 'Sensor', variable: 'temperatura', unidad: '°C', conexion: 'CONECTADO',
  nivel: 'OK', valorActual: 24.5, fechaHoraUltimaLectura: '2026-11-10T18:00:00Z', ultimaRecepcion: '2026-11-10T18:00:00Z',
  ahora: '2026-11-10T18:00:02Z', frecuenciaSegundos: 5, umbralAdvertencia: 30, umbralCritico: 35, desconexionSegundos: 15,
};

describe('IotService (contrato con la API IoT)', () => {
  let servicio: IotService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(IotService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('GET /api/iot/estado devuelve el estado actual', () => {
    let nivel = '';
    servicio.estado().subscribe((e) => (nivel = e.nivel));
    http.expectOne('/api/iot/estado').flush(estado);
    expect(nivel).toBe('OK');
  });

  it('GET /api/iot/lecturas envía desde/hasta en ISO-8601 UTC y el límite', () => {
    const desde = new Date('2026-11-10T17:00:00.000Z');
    const hasta = new Date('2026-11-10T18:00:00.000Z');
    servicio.lecturas(desde, hasta).subscribe();
    const req = http.expectOne((r) => r.url === '/api/iot/lecturas');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('desde')).toBe('2026-11-10T17:00:00.000Z');
    expect(req.request.params.get('hasta')).toBe('2026-11-10T18:00:00.000Z');
    expect(req.request.params.get('limite')).toBe('2000');
    req.flush([]);
  });

  it('GET /api/iot/resumen envía el rango', () => {
    servicio.resumen(new Date('2026-11-10T17:00:00.000Z'), new Date('2026-11-10T18:00:00.000Z')).subscribe();
    const req = http.expectOne((r) => r.url === '/api/iot/resumen');
    expect(req.request.params.get('desde')).toBe('2026-11-10T17:00:00.000Z');
    req.flush({});
  });

  it('GET /api/iot/alertas envía el límite', () => {
    servicio.alertas(10).subscribe();
    const req = http.expectOne((r) => r.url === '/api/iot/alertas');
    expect(req.request.params.get('limite')).toBe('10');
    req.flush([]);
  });

  it('convierte un 400 de la API en un ApiError legible', () => {
    let error: ApiError | undefined;
    servicio.lecturas(new Date(), new Date()).subscribe({ error: (e) => (error = e) });
    http.expectOne((r) => r.url === '/api/iot/lecturas').flush(
      { status: 400, mensaje: "'desde' no puede ser posterior a 'hasta'" }, { status: 400, statusText: 'Bad Request' });
    expect(error?.status).toBe(400);
    expect(error?.mensaje).toContain('desde');
  });

  it('informa cuando no hay conexión con el servidor', () => {
    let error: ApiError | undefined;
    servicio.estado().subscribe({ error: (e) => (error = e) });
    http.expectOne('/api/iot/estado').error(new ProgressEvent('error'));
    expect(error?.status).toBe(0);
    expect(error?.mensaje).toContain('No se pudo conectar');
  });
});
