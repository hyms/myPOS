import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';

import { buildComprobanteHTML, type ComprobanteData } from './ComprobanteRenderer';

export async function generarYCompartirComprobante(data: ComprobanteData): Promise<void> {
  const html = buildComprobanteHTML(data);
  const { uri } = await Print.printToFileAsync({ html });
  await Sharing.shareAsync(uri, { UTI: '.pdf', mimeType: 'application/pdf' });
}
