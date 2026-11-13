import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';
import { Directory, File, Paths } from 'expo-file-system';

import { buildComprobanteHTML, type ComprobanteData } from './ComprobanteRenderer';

function buildFileName(data: ComprobanteData): string {
  const ts = new Date()
    .toISOString()
    .replace(/[-:]/g, '')
    .replace(/\..+/, '');
  return `${data.tipo.toLowerCase()}-${data.transaccionId}-${ts}.pdf`;
}

function getComprobantesDir(): Directory {
  return new Directory(Paths.document, 'comprobantes');
}

async function safeShare(file: File): Promise<void> {
  try {
    const isAvailable = await Sharing.isAvailableAsync();
    if (!isAvailable) return;
    await Sharing.shareAsync(file.uri, {
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

  const source = new File(tempUri);

  const dir = getComprobantesDir();
  if (!dir.exists) {
    dir.create({ intermediates: true, idempotent: true });
  }

  const target = new File(dir, buildFileName(data));
  if (target.exists) {
    target.delete();
  }
  target.create();

  const bytes = await source.bytes();
  target.write(bytes);

  try {
    source.delete();
  } catch {
    // temp file may be in a protected location; not critical
  }

  await safeShare(target);
}
