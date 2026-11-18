export type NivelIot = 'SIN_DATOS' | 'OK' | 'ADVERTENCIA' | 'CRITICO';
export type ConexionIot = 'SIN_DATOS' | 'CONECTADO' | 'DESCONECTADO';
export type Severidad = 'INFO' | 'ADVERTENCIA' | 'CRITICA';
export type TipoAlerta = 'ADVERTENCIA' | 'CRITICA' | 'ANOMALIA' | 'DESCONEXION' | 'RECONEXION' | 'NORMALIZACION';

/** GET /api/iot/estado */
export interface EstadoIot {
  dispositivo: string;
  nombre: string;
  variable: string;
  unidad: string;
  conexion: ConexionIot;
  nivel: NivelIot;
  valorActual: number | null;
  fechaHoraUltimaLectura: string | null;
  ultimaRecepcion: string | null;
  /** Hora del servidor al responder: evita depender del reloj del navegador. */
  ahora: string;
  frecuenciaSegundos: number;
  umbralAdvertencia: number;
  umbralCritico: number;
  desconexionSegundos: number;
}

/** GET /api/iot/lecturas */
export interface LecturaIot {
  id: number;
  dispositivo: string;
  fechaHora: string;
  variable: string;
  unidad: string;
  valor: number;
  anomala: boolean;
  motivoAnomalia: string | null;
}

/** GET /api/iot/resumen (mínimo, máximo y promedio solo con lecturas no anómalas). */
export interface ResumenIot {
  desde: string;
  hasta: string;
  lecturas: number;
  anomalas: number;
  minimo: number | null;
  maximo: number | null;
  promedio: number | null;
}

/** GET /api/iot/alertas */
export interface AlertaIot {
  id: number;
  dispositivoId: number;
  lecturaId: number | null;
  tipo: TipoAlerta;
  severidad: Severidad;
  mensaje: string;
  fechaHora: string;
}

/** El backend limita el historial a 2000 puntos (≈ 2.7 h a 5 s), por eso el rango máximo es 2 h. */
export const RANGOS: { etiqueta: string; minutos: number }[] = [
  { etiqueta: '15 minutos', minutos: 15 },
  { etiqueta: '1 hora', minutos: 60 },
  { etiqueta: '2 horas', minutos: 120 },
];
