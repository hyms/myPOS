import Toast from 'react-native-toast-message';

type Variant = 'info' | 'success' | 'error' | 'warning';

interface ShowOptions {
  readonly title: string;
  readonly message?: string;
  readonly variant?: Variant;
  readonly visibilityTime?: number;
}

const COLORS: Record<Variant, string> = {
  info: '#8b5cf6',
  success: '#16a34a',
  error: '#dc2626',
  warning: '#f59e0b',
};

export const ToastService = {
  show({ title, message, variant = 'info', visibilityTime = 2500 }: ShowOptions): void {
    Toast.show({
      type: 'custom',
      text1: title,
      text2: message,
      visibilityTime,
      position: 'bottom',
      bottomOffset: 80,
      props: { variant, color: COLORS[variant] },
    });
  },

  success(title: string, message?: string): void {
    ToastService.show({ title, message, variant: 'success' });
  },

  error(title: string, message?: string): void {
    ToastService.show({ title, message, variant: 'error', visibilityTime: 4000 });
  },

  warning(title: string, message?: string): void {
    ToastService.show({ title, message, variant: 'warning' });
  },

  info(title: string, message?: string): void {
    ToastService.show({ title, message, variant: 'info' });
  },
};
