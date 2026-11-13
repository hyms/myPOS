import * as DocumentPicker from 'expo-document-picker';
import { Directory, File, Paths } from 'expo-file-system';

import { closeDatabase } from '@/data/database/client';
import { resetRepositoriesForTests } from '@/data/repositories/container';

function getDbFile(): File {
  return new File(Paths.document, 'SQLite', 'tpv.db');
}

function getSqliteDir(): Directory {
  return new Directory(Paths.document, 'SQLite');
}

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

  const sqliteDir = getSqliteDir();
  if (!sqliteDir.exists) {
    sqliteDir.create({ intermediates: true, idempotent: true });
  }

  const tmp = new File(Paths.cache, `restore-${Date.now()}.db`);
  const target = getDbFile();
  const source = new File(origen);

  await source.copy(tmp);
  await tmp.copy(target);
  tmp.delete();

  return { origen };
}
