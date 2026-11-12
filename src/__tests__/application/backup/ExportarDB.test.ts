import { exportarBackup } from '@/application/backup/ExportarDB';

import {
  mockDocumentPickerResult,
  resetDocumentPicker,
} from '@/__tests__/__mocks__/expo-document-picker';
import { resetFileSystem } from '@/__tests__/__mocks__/expo-file-system';

// These modules are auto-mocked via jest config pointing to __mocks__

describe('exportarBackup', () => {
  beforeEach(() => {
    resetFileSystem();
  });

  it('lanza error si no existe la DB', async () => {
    await expect(exportarBackup()).rejects.toThrow('No se encontró');
  });
});
