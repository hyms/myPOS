import { useEffect, useState } from 'react';

import { getRepositories } from '@/data/repositories/container';
import { getDatabase } from '@/data/database/client';

export function useDatabaseStatus() {
  const [ready, setReady] = useState(false);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    try {
      getDatabase();
      getRepositories();
      setReady(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }, []);
  return { ready, error };
}
