const storage = new Map<string, string>();

const AsyncStorage = {
  getItem: jest.fn(async (key: string): Promise<string | null> => {
    return storage.get(key) ?? null;
  }),

  setItem: jest.fn(async (key: string, value: string): Promise<void> => {
    storage.set(key, value);
  }),

  removeItem: jest.fn(async (key: string): Promise<void> => {
    storage.delete(key);
  }),

  clear: jest.fn(async (): Promise<void> => {
    storage.clear();
  }),
};

export function resetAsyncStorage(): void {
  storage.clear();
  jest.clearAllMocks();
}

export default AsyncStorage;
