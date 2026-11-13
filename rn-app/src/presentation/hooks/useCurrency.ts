import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { formatCurrency } from '@/shared/utils/format';

export function useCurrency() {
  const currency = useSettingsStore((s) => s.currency);
  return {
    currency,
    format: (amount: number, fractionDigits = 2) => formatCurrency(amount, currency, fractionDigits),
  };
}

export function useIsHydrated(): boolean {
  return useSettingsStore((s) => s.hydrated);
}
