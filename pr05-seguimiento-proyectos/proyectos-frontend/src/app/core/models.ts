export type Estado = 'REGISTRADO' | 'EN_PROGRESO' | 'EN_REVISION' | 'ENTREGADO';

export const ESTADOS: Estado[] = ['REGISTRADO', 'EN_PROGRESO', 'EN_REVISION', 'ENTREGADO'];

export interface Proyecto {
  id: number;
  titulo: string;
  descripcion: string;
  integrantes: string;
  asesor: string;
  estado: Estado;
  estadosPermitidos: Estado[];
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface ProyectoRequest {
  titulo: string;
  descripcion: string;
  integrantes: string;
  asesor: string;
}

export interface Salud {
  status: string;
  servicio: string;
  timestamp: string;
}

/** Cuerpo de error uniforme que devuelve la API (ver openapi.yaml). */
export interface ApiError {
  timestamp?: string;
  status: number;
  error?: string;
  mensaje: string;
  ruta?: string;
  campos?: Record<string, string> | null;
}
