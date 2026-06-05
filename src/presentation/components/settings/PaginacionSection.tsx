import React, { memo, useCallback } from 'react';
import { View } from 'react-native';

import { Input } from '@/presentation/components/ui/Input';
import { Button } from '@/presentation/components/ui/Button';
import { Card } from '@/presentation/components/ui/Card';
import { useSettingsPageSize } from '@/presentation/hooks/useSettingsPageSize';

function PaginacionSectionComponent() {
  const { draft, setDraft, save } = useSettingsPageSize();
  const onChange = useCallback((t: string) => setDraft(t.replace(/[^0-9]/g, '')), [setDraft]);
  return (
    <Card>
      <View className="flex-row items-end gap-3">
        <View className="flex-1">
          <Input
            label="Tamaño de página"
            value={draft}
            onChangeText={onChange}
            keyboardType="number-pad"
          />
        </View>
        <View className="pb-1">
          <Button title="Guardar" onPress={save} />
        </View>
      </View>
    </Card>
  );
}

export const PaginacionSection = memo(PaginacionSectionComponent);
