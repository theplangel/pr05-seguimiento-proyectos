import { LecturaIot } from '../core/iot.models';
import { construirGrafica } from './iot.grafica';

const lectura = (seg: number, valor: number, anomala = false, motivo: string | null = null): LecturaIot => ({
  id: seg, dispositivo: 'sensor-lab-01',
  fechaHora: new Date(Date.UTC(2026, 10, 10, 18, 0, seg)).toISOString(),
  variable: 'temperatura', unidad: '°C', valor, anomala, motivoAnomalia: motivo,
});

describe('construirGrafica', () => {
  it('sin lecturas devuelve una gráfica vacía', () => {
    expect(construirGrafica([], 30, 35, 15).vacia).toBe(true);
  });

  it('une las lecturas válidas en un solo segmento', () => {
    const g = construirGrafica([lectura(0, 24), lectura(5, 25), lectura(10, 26)], 30, 35, 15);
    expect(g.vacia).toBe(false);
    expect(g.segmentos).toHaveLength(1);
    expect(g.segmentos[0].split(' ')).toHaveLength(3);
  });

  it('corta la línea cuando hay un hueco mayor al límite de desconexión', () => {
    const g = construirGrafica([lectura(0, 24), lectura(5, 25), lectura(60, 26), lectura(65, 26)], 30, 35, 15);
    expect(g.segmentos).toHaveLength(2);
  });

  it('un solo punto se dibuja como segmento visible', () => {
    const g = construirGrafica([lectura(0, 24)], 30, 35, 15);
    expect(g.segmentos[0].split(' ')).toHaveLength(2);
  });

  it('las lecturas anómalas no entran en la línea ni distorsionan la escala', () => {
    const g = construirGrafica([lectura(0, 24), lectura(5, 120, true, 'FUERA_DE_RANGO'), lectura(10, 25)], 30, 35, 15);
    expect(g.anomalas).toHaveLength(1);
    expect(g.anomalas[0].texto).toContain('FUERA_DE_RANGO');
    expect(g.anomalas[0].y).toBeGreaterThanOrEqual(0);
    expect(g.anomalas[0].y).toBeLessThanOrEqual(g.alto);
    expect(g.segmentos[0].split(' ')).toHaveLength(2);
    expect(Number(g.ejeY[g.ejeY.length - 1].etiqueta)).toBeLessThan(60);
  });

  it('el umbral crítico queda por encima (menor y) del de advertencia', () => {
    const g = construirGrafica([lectura(0, 24), lectura(5, 25)], 30, 35, 15);
    expect(g.lineaCritico).toBeLessThan(g.lineaAdvertencia);
  });

  it('un valor más alto se dibuja más arriba', () => {
    const g = construirGrafica([lectura(0, 20), lectura(5, 33)], 30, 35, 15);
    const [p1, p2] = g.segmentos[0].split(' ').map((p) => Number(p.split(',')[1]));
    expect(p2).toBeLessThan(p1);
  });
});
