import { Redirect } from 'expo-router';
import { useShallow } from 'zustand/react/shallow';

import { useSessionStore } from '@/presentation/stores/sessionStore';

export default function RootIndex() {
  const { pinRequired, unlocked } = useSessionStore(
    useShallow((s) => ({ pinRequired: s.pinRequired, unlocked: s.unlocked })),
  );

  if (pinRequired && !unlocked) {
    return <Redirect href="/lock" />;
  }
  return <Redirect href="/(tabs)/caja" />;
}
