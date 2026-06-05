import { exportarBackup } from '@/application/backup/ExportarDB';

import { resetFileSystem } from 'expo-file-system/legacy';
import { mockDocumentPickerResult, resetDocumentPicker } from 'expo-document-picker';

// These modules are auto-mocked via jest config pointing to __mocks__

describe('exportarBackup', () => {
  beforeEach(() => {
    resetFileSystem();
  });

  it('lanza error si no existe la DB', async () => {
    await expect(exportarBackup()).rejects.toThrow('No se encontró');
  });
});
