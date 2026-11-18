import { LecturaIot } from '../core/iot.models';

export interface PuntoAnomalo {
  x: number;
  y: number;
  texto: string;
}

export interface GraficaIot {
  ancho: number;
  alto: number;
  vacia: boolean;
  /** Series de puntos "x,y x,y" para <polyline>; se corta donde hubo desconexión. */
  segmentos: string[];
  anomalas: PuntoAnomalo[];
  ejeY: { y: number; etiqueta: string }[];
  lineaAdvertencia: number;
  lineaCritico: number;
  izquierda: number;
  derecha: number;
  etiquetaInicio: string;
  etiquetaFin: string;
}

const ANCHO = 640;
const ALTO = 240;
const MI = 44; // margen izquierdo
const MD = 12;
const MS = 12;
const MB = 28;

function hora(ms: number): string {
  return new Date(ms).toLocaleTimeString('es-MX', { hour12: false });
}

/**
 * Convierte lecturas (orden cronológico) en geometría SVG. Función pura para poder probarla sin DOM.
 * - Las lecturas anómalas no se unen a la línea ni definen la escala: se marcan aparte en el borde visible.
 * - Un hueco mayor que `huecoSeg` (desconexión) corta la línea en segmentos.
 */
export function construirGrafica(
  lecturas: LecturaIot[],
  umbralAdvertencia: number,
  umbralCritico: number,
  huecoSeg: number,
): GraficaIot {
  const base = { ancho: ANCHO, alto: ALTO, izquierda: MI, derecha: ANCHO - MD };
  if (lecturas.length === 0) {
    return { ...base, vacia: true, segmentos: [], anomalas: [], ejeY: [], lineaAdvertencia: 0, lineaCritico: 0,
      etiquetaInicio: '', etiquetaFin: '' };
  }
  const tiempos = lecturas.map((l) => Date.parse(l.fechaHora));
  const t0 = Math.min(...tiempos);
  const t1 = Math.max(Math.max(...tiempos), t0 + 1000);
  const validas = lecturas.filter((l) => !l.anomala);
  const valores = validas.map((l) => l.valor);
  const ymin = Math.floor(Math.min(...valores, umbralAdvertencia - 8));
  const ymax = Math.ceil(Math.max(...valores, umbralCritico + 2));

  const x = (t: number) => MI + ((t - t0) / (t1 - t0)) * (ANCHO - MI - MD);
  const y = (v: number) => MS + (1 - (v - ymin) / (ymax - ymin)) * (ALTO - MS - MB);
  const r = (n: number) => Math.round(n * 10) / 10;

  const segmentos: string[] = [];
  let actual: string[] = [];
  let tPrev: number | null = null;
  for (const l of validas) {
    const t = Date.parse(l.fechaHora);
    if (tPrev !== null && t - tPrev > huecoSeg * 1000) {
      segmentos.push(cerrar(actual));
      actual = [];
    }
    actual.push(`${r(x(t))},${r(y(l.valor))}`);
    tPrev = t;
  }
  if (actual.length) {
    segmentos.push(cerrar(actual));
  }

  const anomalas: PuntoAnomalo[] = lecturas
    .filter((l) => l.anomala)
    .map((l) => ({
      x: r(x(Date.parse(l.fechaHora))),
      y: r(Math.min(Math.max(y(l.valor), MS), ALTO - MB)),
      texto: `${l.valor} ${l.unidad} · ${l.motivoAnomalia ?? 'anómala'}`,
    }));

  const ejeY = [0, 1, 2, 3, 4].map((i) => {
    const v = ymin + ((ymax - ymin) * i) / 4;
    return { y: r(y(v)), etiqueta: String(Math.round(v)) };
  });

  return {
    ...base,
    vacia: false,
    segmentos,
    anomalas,
    ejeY,
    lineaAdvertencia: r(y(umbralAdvertencia)),
    lineaCritico: r(y(umbralCritico)),
    etiquetaInicio: hora(t0),
    etiquetaFin: hora(t1),
  };
}

/** Un solo punto se duplica para que <polyline> lo dibuje (con trazo redondeado). */
function cerrar(puntos: string[]): string {
  return puntos.length === 1 ? `${puntos[0]} ${puntos[0]}` : puntos.join(' ');
}
