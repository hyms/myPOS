import dayjs from 'dayjs';

export function mesActual(): string {
  return dayjs().format('YYYY-MM');
}

export function anioActual(): string {
  return dayjs().format('YYYY');
}

export function formatFecha(iso: string): string {
  return dayjs(iso).format('DD/MM/YYYY HH:mm');
}

export function formatFechaCorta(iso: string): string {
  return dayjs(iso).format('DD/MM/YY');
}
