import * as FileSystem from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';

const DB_PATH = `${FileSystem.documentDirectory}SQLite/tpv.db`;

export async function exportarBackup(): Promise<string> {
  const info = await FileSystem.getInfoAsync(DB_PATH);
  if (!info.exists) {
    throw new Error('No se encontró la base de datos local.');
  }
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  const hh = String(now.getHours()).padStart(2, '0');
  const mi = String(now.getMinutes()).padStart(2, '0');
  const fileName = `tpv-${yyyy}${mm}${dd}-${hh}${mi}.db`;
  const backupDir = `${FileSystem.documentDirectory}backups/`;
  const dirInfo = await FileSystem.getInfoAsync(backupDir);
  if (!dirInfo.exists) {
    await FileSystem.makeDirectoryAsync(backupDir, { intermediates: true });
  }
  const dest = `${backupDir}${fileName}`;
  await FileSystem.copyAsync({ from: DB_PATH, to: dest });
  await Sharing.shareAsync(dest, { dialogTitle: 'Exportar base de datos' });
  return dest;
}
