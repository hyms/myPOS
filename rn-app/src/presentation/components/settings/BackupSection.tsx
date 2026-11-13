import React, { memo } from 'react';
import { View } from 'react-native';

import { Button } from '@/presentation/components/ui/Button';
import { Card } from '@/presentation/components/ui/Card';
import { ConfirmDialog } from '@/presentation/components/feedback/ConfirmDialog';
import { useSettingsBackup } from '@/presentation/hooks/useSettingsBackup';

function BackupSectionComponent() {
  const { busy, restoreOpen, setRestoreOpen, exportDb, restoreDb } = useSettingsBackup();

  return (
    <>
      <Card className="gap-3">
        <Button
          title="Exportar base de datos"
          onPress={exportDb}
          busy={busy === 'export'}
          fullWidth
        />
        <Button
          title="Restaurar desde archivo…"
          variant="danger"
          onPress={() => setRestoreOpen(true)}
          busy={busy === 'restore'}
          fullWidth
        />
      </Card>
      <ConfirmDialog
        visible={restoreOpen}
        title="Restaurar base de datos"
        message="Esto sobrescribirá todos los datos actuales. ¿Continuar?"
        destructive
        onCancel={() => setRestoreOpen(false)}
        onConfirm={restoreDb}
        busy={busy === 'restore'}
      />
    </>
  );
}

export const BackupSection = memo(BackupSectionComponent);
