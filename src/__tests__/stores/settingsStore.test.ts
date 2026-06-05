import { useSettingsStore } from '@/presentation/stores/settingsStore';

describe('settingsStore', () => {
  beforeEach(() => {
    useSettingsStore.setState({
      currency: 'Bs',
      pageSize: 15,
      biometricEnabled: false,
      hydrated: false,
    });
  });

  it('tiene valores por defecto', () => {
    const s = useSettingsStore.getState();
    expect(s.currency).toBe('Bs');
    expect(s.pageSize).toBe(15);
    expect(s.biometricEnabled).toBe(false);
    expect(s.hydrated).toBe(false);
  });

  it('setCurrency cambia la moneda', () => {
    useSettingsStore.getState().setCurrency('$');
    expect(useSettingsStore.getState().currency).toBe('$');
  });

  it('setPageSize valida rango', () => {
    useSettingsStore.getState().setPageSize(200);
    expect(useSettingsStore.getState().pageSize).toBe(100);
    useSettingsStore.getState().setPageSize(-5);
    expect(useSettingsStore.getState().pageSize).toBe(1);
    useSettingsStore.getState().setPageSize(10.7);
    expect(useSettingsStore.getState().pageSize).toBe(10);
  });

  it('setBiometricEnabled', () => {
    useSettingsStore.getState().setBiometricEnabled(true);
    expect(useSettingsStore.getState().biometricEnabled).toBe(true);
  });

  it('hydrate marca como hydrated', () => {
    useSettingsStore.getState().hydrate({
      currency: '€',
      pageSize: 25,
      biometricEnabled: true,
    });
    const s = useSettingsStore.getState();
    expect(s.currency).toBe('€');
    expect(s.pageSize).toBe(25);
    expect(s.biometricEnabled).toBe(true);
    expect(s.hydrated).toBe(true);
  });
});
