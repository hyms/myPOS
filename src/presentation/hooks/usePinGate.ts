import { useCallback, useState } from 'react';

import { PinService } from '@/infrastructure/security/PinService';
import { ToastService } from '@/infrastructure/toast/ToastService';

interface PinGateOptions {
  readonly onSuccess: () => void | Promise<void>;
  readonly title?: string;
}

export function usePinGate() {
  const [promptOpen, setPromptOpen] = useState(false);
  const [pendingAction, setPendingAction] = useState<PinGateOptions | null>(null);
  const [busy, setBusy] = useState(false);

  const request = useCallback(async (options: PinGateOptions) => {
    const hasPin = await PinService.hasPin();
    if (!hasPin) {
      await options.onSuccess();
      return;
    }
    setPendingAction(options);
    setPromptOpen(true);
  }, []);

  const cancel = useCallback(() => {
    setPromptOpen(false);
    setPendingAction(null);
  }, []);

  const submit = useCallback(
    async (pin: string) => {
      if (!pendingAction) return;
      setBusy(true);
      try {
        const ok = await PinService.verifyPin(pin);
        if (!ok) {
          ToastService.error('PIN incorrecto');
          return;
        }
        setPromptOpen(false);
        const action = pendingAction;
        setPendingAction(null);
        await action.onSuccess();
      } finally {
        setBusy(false);
      }
    },
    [pendingAction],
  );

  return { promptOpen, busy, request, cancel, submit };
}
