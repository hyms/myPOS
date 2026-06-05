import Toast from 'react-native-toast-message';

import { DARK_PALETTE } from '@/presentation/theme/tokens';

type Variant = 'info' | 'success' | 'error' | 'warning';

interface ShowOptions {
  readonly title: string;
  readonly message?: string;
  readonly variant?: Variant;
  readonly visibilityTime?: number;
}

const COLORS: Record<Variant, string> = {
  info: DARK_PALETTE.info,
  success: DARK_PALETTE.success,
  error: DARK_PALETTE.danger,
  warning: DARK_PALETTE.warning,
};

export const ToastService = {
  show({ title, message, variant = 'info', visibilityTime = 2500 }: ShowOptions): void {
    Toast.show({
      type: 'custom',
      text1: title,
      ...(message !== undefined ? { text2: message } : {}),
      visibilityTime,
      position: 'bottom',
      bottomOffset: 80,
      props: { variant, color: COLORS[variant] },
    });
  },

  success(title: string, message?: string): void {
    ToastService.show(
      message !== undefined
        ? { title, message, variant: 'success' }
        : { title, variant: 'success' },
    );
  },

  error(title: string, message?: string): void {
    ToastService.show(
      message !== undefined
        ? { title, message, variant: 'error', visibilityTime: 4000 }
        : { title, variant: 'error', visibilityTime: 4000 },
    );
  },

  warning(title: string, message?: string): void {
    ToastService.show(
      message !== undefined
        ? { title, message, variant: 'warning' }
        : { title, variant: 'warning' },
    );
  },

  info(title: string, message?: string): void {
    ToastService.show(
      message !== undefined
        ? { title, message, variant: 'info' }
        : { title, variant: 'info' },
    );
  },
};
