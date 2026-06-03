import * as DocumentPicker from 'expo-document-picker';
import * as FileSystem from 'expo-file-system/legacy';

import { closeDatabase } from '@/data/database/client';
import { resetRepositoriesForTests } from '@/data/repositories/container';

const DB_PATH = `${FileSystem.documentDirectory}SQLite/tpv.db`;

export interface RestaurarDBResult {
  readonly origen: string;
}

export async function restaurarBackup(): Promise<RestaurarDBResult> {
  const pick = await DocumentPicker.getDocumentAsync({
    type: '*/*',
    copyToCacheDirectory: true,
  });
  if (pick.canceled || !pick.assets[0]) {
    throw new Error('Selección cancelada.');
  }
  const origen = pick.assets[0].uri;

  closeDatabase();
  resetRepositoriesForTests();

  const targetDir = `${FileSystem.documentDirectory}SQLite/`;
  const dirInfo = await FileSystem.getInfoAsync(targetDir);
  if (!dirInfo.exists) {
    await FileSystem.makeDirectoryAsync(targetDir, { intermediates: true });
  }

  const tmp = `${FileSystem.cacheDirectory}restore-${Date.now()}.db`;
  await FileSystem.copyAsync({ from: origen, to: tmp });
  await FileSystem.copyAsync({ from: tmp, to: DB_PATH, options: { overwrite: true } });
  await FileSystem.deleteAsync(tmp, { idempotent: true });

  return { origen };
}
