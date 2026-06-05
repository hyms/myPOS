import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';
import { Directory, File, Paths } from 'expo-file-system';

import { buildComprobanteHTML, type ComprobanteData } from './ComprobanteRenderer';

function getComprobantesDir(): Directory {
  const dir = new Directory(Paths.document, 'comprobantes');
  if (!dir.exists) {
    dir.create();
  }
  return dir;
}

function buildFileName(data: ComprobanteData): string {
  const ts = new Date()
    .toISOString()
    .replace(/[-:]/g, '')
    .replace(/\..+/, '');
  return `${data.tipo.toLowerCase()}-${data.transaccionId}-${ts}.pdf`;
}

async function safeShare(uri: string): Promise<void> {
  try {
    const isAvailable = await Sharing.isAvailableAsync();
    if (!isAvailable) return;
    await Sharing.shareAsync(uri, {
      dialogTitle: 'Compartir comprobante',
      mimeType: 'application/pdf',
      UTI: '.pdf',
    });
  } catch {
    // share intent failed (no apps, user cancelled, FileProvider missing)
  }
}

export async function generarYCompartirComprobante(data: ComprobanteData): Promise<void> {
  const html = buildComprobanteHTML(data);
  const { uri: tempUri } = await Print.printToFileAsync({ html });

  const dir = getComprobantesDir();
  const target = new File(dir, buildFileName(data));
  if (target.exists) {
    target.delete();
  }
  const source = new File(tempUri);
  await source.move(target);

  await safeShare(target.uri);
}
