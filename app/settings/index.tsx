import React from 'react';
import { ScrollView } from 'react-native';

import { SectionHeader } from '@/presentation/components/settings/SectionHeader';
import { DivisaSection } from '@/presentation/components/settings/DivisaSection';
import { PaginacionSection } from '@/presentation/components/settings/PaginacionSection';
import { SeguridadSection } from '@/presentation/components/settings/SeguridadSection';
import { BackupSection } from '@/presentation/components/settings/BackupSection';

export default function SettingsIndex() {
  return (
    <ScrollView
      className="flex-1 bg-surface"
      contentContainerClassName="gap-3 p-4 pb-10"
      keyboardShouldPersistTaps="handled"
    >
      <SectionHeader
        title="Divisa"
        description="Formato visual de los montos en toda la app."
      />
      <DivisaSection />

      <SectionHeader
        title="Paginación"
        description="Cantidad de filas por consulta en las listas históricas (gastos, transacciones, etc.)."
      />
      <PaginacionSection />

      <SectionHeader
        title="Seguridad"
        description="PIN de acceso y desbloqueo biométrico."
      />
      <SeguridadSection />

      <SectionHeader
        title="Copia de seguridad"
        description="Exporta o restaura la base de datos del dispositivo."
      />
      <BackupSection />
    </ScrollView>
  );
}
