import { ToastService } from '@/infrastructure/toast/ToastService';
import { STOCK_ALERT_TOAST_TIME } from '@/shared/constants';

interface StockAlertItem {
  readonly id: number;
  readonly nombre: string;
  readonly stockActual: number;
  readonly stockMinimo: number;
}

export function useStockAlert() {
  return {
    notify(items: ReadonlyArray<StockAlertItem>): void {
      if (items.length === 0) return;
      const first = items[0];
      if (!first) return;
      if (items.length === 1) {
        ToastService.warning(
          'Stock bajo',
          `${first.nombre}: ${first.stockActual} / mínimo ${first.stockMinimo}`,
        );
        return;
      }
      ToastService.show({
        title: 'Stock bajo',
        message: `${items.length} productos por debajo del mínimo (${first.nombre} y otros).`,
        variant: 'warning',
        visibilityTime: STOCK_ALERT_TOAST_TIME,
      });
    },
  };
}
