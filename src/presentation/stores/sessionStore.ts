import { create } from 'zustand';

interface SessionState {
  unlocked: boolean;
  pinRequired: boolean;
  setUnlocked: (v: boolean) => void;
  setPinRequired: (v: boolean) => void;
}

export const useSessionStore = create<SessionState>((set) => ({
  unlocked: false,
  pinRequired: false,
  setUnlocked: (unlocked) => set({ unlocked }),
  setPinRequired: (pinRequired) => set({ pinRequired }),
}));
