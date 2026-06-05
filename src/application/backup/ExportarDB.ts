import { Directory, File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';

function getDbFile(): File {
  return new File(Paths.document, 'SQLite', 'tpv.db');
}

function getBackupDir(): Directory {
  return new Directory(Paths.document, 'backups');
}

function buildFileName(): string {
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  const hh = String(now.getHours()).padStart(2, '0');
  const mi = String(now.getMinutes()).padStart(2, '0');
  return `tpv-${yyyy}${mm}${dd}-${hh}${mi}.db`;
}

export async function exportarBackup(): Promise<string> {
  const source = getDbFile();
  if (!source.exists) {
    throw new Error('No se encontró la base de datos local.');
  }

  const dir = getBackupDir();
  if (!dir.exists) {
    dir.create({ intermediates: true, idempotent: true });
  }

  const dest = new File(dir, buildFileName());
  await source.copy(dest);

  await Sharing.shareAsync(dest.uri, { dialogTitle: 'Exportar base de datos' });
  return dest.uri;
}
