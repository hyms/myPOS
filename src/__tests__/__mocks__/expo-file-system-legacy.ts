// In-memory file system mock
const files = new Map<string, string | Uint8Array>();
const dirs = new Set<string>();

export const documentDirectory = '/mock/document/';
export const cacheDirectory = '/mock/cache/';

export async function getInfoAsync(
  uri: string,
  _options?: { size?: boolean },
): Promise<{ exists: boolean; size?: number }> {
  const exists = files.has(uri) || dirs.has(uri);
  const size = files.has(uri) ? new TextEncoder().encode(files.get(uri)).length : 0;
  return { exists, size };
}

export async function makeDirectoryAsync(
  _uri: string,
  _options?: { intermediates?: boolean },
): Promise<void> {
  return;
}

export async function copyAsync(options: {
  from: string;
  to: string;
  options?: { overwrite?: boolean };
}): Promise<void> {
  const content = files.get(options.from);
  if (content !== undefined) {
    files.set(options.to, content);
  }
}

export async function deleteAsync(
  uri: string,
  _options?: { idempotent?: boolean },
): Promise<void> {
  files.delete(uri);
  dirs.delete(uri);
}

export async function readAsStringAsync(uri: string): Promise<string> {
  return files.get(uri) as string;
}

export async function writeAsStringAsync(uri: string, content: string): Promise<void> {
  files.set(uri, content);
}

export function resetFileSystem(): void {
  files.clear();
  dirs.clear();
  dirs.add(documentDirectory);
  dirs.add(cacheDirectory);
}
