import { useSessionStore } from '@/presentation/stores/sessionStore';

describe('sessionStore', () => {
  beforeEach(() => {
    useSessionStore.setState({ unlocked: false, pinRequired: false });
  });

  it('estado inicial: bloqueado y sin PIN requerido', () => {
    const s = useSessionStore.getState();
    expect(s.unlocked).toBe(false);
    expect(s.pinRequired).toBe(false);
  });

  it('setUnlocked desbloquea', () => {
    useSessionStore.getState().setUnlocked(true);
    expect(useSessionStore.getState().unlocked).toBe(true);
  });

  it('setPinRequired marca PIN requerido', () => {
    useSessionStore.getState().setPinRequired(true);
    expect(useSessionStore.getState().pinRequired).toBe(true);
  });
});
