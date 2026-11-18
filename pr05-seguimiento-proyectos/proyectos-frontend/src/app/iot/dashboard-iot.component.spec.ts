import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { AlertaIot, EstadoIot, LecturaIot, ResumenIot } from '../core/iot.models';
import { DashboardIotComponent, PERIODO_MS } from './dashboard-iot.component';

const estado = (o: Partial<EstadoIot> = {}): EstadoIot => ({
  dispositivo: 'sensor-lab-01', nombre: 'Sensor de temperatura del laboratorio', variable: 'temperatura', unidad: '°C',
  conexion: 'CONECTADO', nivel: 'OK', valorActual: 24.5, fechaHoraUltimaLectura: '2026-11-10T18:00:00Z',
  ultimaRecepcion: '2026-11-10T18:00:00Z', ahora: '2026-11-10T18:00:03Z', frecuenciaSegundos: 5,
  umbralAdvertencia: 30, umbralCritico: 35, desconexionSegundos: 15, ...o,
});
const lectura = (seg: number, valor: number, anomala = false): LecturaIot => ({
  id: seg + 1, dispositivo: 'sensor-lab-01', fechaHora: new Date(Date.UTC(2026, 10, 10, 18, 0, seg)).toISOString(),
  variable: 'temperatura', unidad: '°C', valor, anomala, motivoAnomalia: anomala ? 'FUERA_DE_RANGO' : null,
});
const resumen: ResumenIot = { desde: 'a', hasta: 'b', lecturas: 3, anomalas: 1, minimo: 24, maximo: 26, promedio: 25 };
const alerta = (id: number, tipo: AlertaIot['tipo'], sev: AlertaIot['severidad'], mensaje: string): AlertaIot => ({
  id, dispositivoId: 1, lecturaId: null, tipo, severidad: sev, mensaje, fechaHora: '2026-11-10T18:00:00Z',
});

describe('DashboardIotComponent', () => {
  let fixture: ComponentFixture<DashboardIotComponent>;
  let http: HttpTestingController;

  const q = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const qa = (id: string) => fixture.nativeElement.querySelectorAll(`[data-testid="${id}"]`) as NodeListOf<HTMLElement>;

  function responder(datos: { estado?: EstadoIot; lecturas?: LecturaIot[]; alertas?: AlertaIot[] } = {}) {
    http.expectOne('/api/iot/estado').flush(datos.estado ?? estado());
    http.expectOne((r) => r.url === '/api/iot/lecturas').flush(datos.lecturas ?? [lectura(0, 24), lectura(5, 25)]);
    http.expectOne((r) => r.url === '/api/iot/resumen').flush(resumen);
    http.expectOne((r) => r.url === '/api/iot/alertas').flush(datos.alertas ?? []);
    fixture.detectChanges();
  }

  function iniciar() {
    TestBed.configureTestingModule({
      imports: [DashboardIotComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(DashboardIotComponent);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    tick(0);
  }

  it('muestra valor, nivel normal y conexión, sin avisos', fakeAsync(() => {
    iniciar();
    responder();
    expect(q('valor')?.textContent).toContain('24.5');
    expect(q('nivel')?.textContent).toContain('Normal');
    expect(q('conexion')?.textContent).toContain('Conectado');
    expect(q('aviso-critico')).toBeNull();
    expect(q('aviso-desconexion')).toBeNull();
    expect(q('grafica')).not.toBeNull();
    expect(q('resumen')?.textContent).toContain('Anómalas');
    fixture.destroy();
  }));

  it('con nivel CRÍTICO muestra un aviso visible', fakeAsync(() => {
    iniciar();
    responder({ estado: estado({ nivel: 'CRITICO', valorActual: 37 }) });
    expect(q('aviso-critico')?.textContent).toContain('CRÍTICO');
    expect(q('nivel')?.textContent).toContain('Crítico');
    fixture.destroy();
  }));

  it('con nivel de ADVERTENCIA muestra un aviso ámbar', fakeAsync(() => {
    iniciar();
    responder({ estado: estado({ nivel: 'ADVERTENCIA', valorActual: 31 }) });
    expect(q('aviso-advertencia')?.textContent).toContain('Advertencia');
    fixture.destroy();
  }));

  it('con el dispositivo desconectado avisa y cuenta los segundos sin datos', fakeAsync(() => {
    iniciar();
    responder({ estado: estado({ conexion: 'DESCONECTADO', ultimaRecepcion: '2026-11-10T17:59:30Z' }) });
    expect(q('aviso-desconexion')?.textContent).toContain('desconectado');
    expect(q('aviso-desconexion')?.textContent).toContain('33 s');
    expect(q('conexion')?.textContent).toContain('Desconectado');
    fixture.destroy();
  }));

  it('lista las alertas y marca las lecturas anómalas en la gráfica', fakeAsync(() => {
    iniciar();
    responder({
      lecturas: [lectura(0, 24), lectura(5, 120, true), lectura(10, 25)],
      alertas: [alerta(2, 'DESCONEXION', 'CRITICA', 'Sin lecturas desde hace 20 s'), alerta(1, 'ANOMALIA', 'ADVERTENCIA', 'Lectura anómala')],
    });
    expect(qa('fila-alerta')).toHaveLength(2);
    expect(qa('fila-alerta')[0].textContent).toContain('DESCONEXION');
    expect(qa('punto-anomalo')).toHaveLength(1);
    expect(qa('fila-lectura')[1].textContent).toContain('Anómala');
    fixture.destroy();
  }));

  it('sin lecturas ni alertas muestra mensajes vacíos comprensibles', fakeAsync(() => {
    iniciar();
    responder({ estado: estado({ conexion: 'SIN_DATOS', nivel: 'SIN_DATOS', valorActual: null, ultimaRecepcion: null }),
      lecturas: [], alertas: [] });
    expect(q('sin-lecturas')).not.toBeNull();
    expect(q('sin-alertas')).not.toBeNull();
    expect(q('valor')?.textContent).toContain('—');
    fixture.destroy();
  }));

  it('vuelve a consultar cada 5 s (lectura periódica)', fakeAsync(() => {
    iniciar();
    responder();
    tick(PERIODO_MS);
    responder();
    tick(PERIODO_MS);
    responder();
    fixture.destroy();
  }));

  it('si la API falla muestra el error, conserva lo último y se recupera', fakeAsync(() => {
    iniciar();
    responder();
    tick(PERIODO_MS);
    http.expectOne('/api/iot/estado').error(new ProgressEvent('error'));
    http.match((r) => r.url.startsWith('/api/iot/')); // forkJoin cancela las demás peticiones al fallar la primera
    fixture.detectChanges();
    expect(q('error')?.textContent).toContain('No se pudo conectar');
    expect(q('valor')?.textContent).toContain('24.5');
    tick(PERIODO_MS);
    responder();
    expect(q('error')).toBeNull();
    fixture.destroy();
  }));

  it('al cambiar el rango consulta de nuevo con una ventana distinta', fakeAsync(() => {
    iniciar();
    responder();
    const select = q('rango') as HTMLSelectElement;
    select.value = '15';
    select.dispatchEvent(new Event('change'));
    tick(0);
    const desde = http.expectOne((r) => r.url === '/api/iot/lecturas').request.params.get('desde') as string;
    const hasta = http.match((r) => r.url === '/api/iot/resumen')[0].request.params.get('hasta') as string;
    const minutos = (Date.parse(hasta) - Date.parse(desde)) / 60000;
    expect(Math.round(minutos)).toBe(16); // 15 min + 1 min de holgura
    http.match(() => true).forEach((r) => r.flush([]));
    fixture.destroy();
  }));
});
