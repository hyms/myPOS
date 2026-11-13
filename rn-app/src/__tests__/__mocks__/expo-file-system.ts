const files = new Map<string, string | Uint8Array>();
const dirs = new Set<string>();

class MockFile {
  constructor(...uris: (string | MockFile | MockDirectory)[]) {
    const parts = uris.map((u) => (typeof u === 'string' ? u.replace(/\/+$/, '') : u.uri));
    this.uri = parts.length > 1 ? parts.join('/').replace(/([^:])\/+/g, '$1/') : (parts[0] ?? '');
  }
  readonly uri: string;
  get exists(): boolean {
    return files.has(this.uri) && !(dirs.has(this.uri + '/') || dirs.has(this.uri));
  }
  get size(): number {
    const v = files.get(this.uri);
    if (v === undefined) return 0;
    if (typeof v === 'string') return new TextEncoder().encode(v).length;
    return v.length;
  }
  create(): void {
    if (files.has(this.uri)) {
      throw new Error('file already exists');
    }
    files.set(this.uri, new Uint8Array());
  }
  delete(): void {
    files.delete(this.uri);
  }
  async bytes(): Promise<Uint8Array> {
    const v = files.get(this.uri);
    if (v === undefined) throw new Error('file does not exist');
    if (typeof v === 'string') return new TextEncoder().encode(v);
    return v;
  }
  async text(): Promise<string> {
    const v = files.get(this.uri);
    if (v === undefined) throw new Error('file does not exist');
    if (typeof v === 'string') return v;
    return new TextDecoder().decode(v);
  }
  async base64(): Promise<string> {
    const v = files.get(this.uri);
    if (v === undefined) throw new Error('file does not exist');
    if (typeof v === 'string') return Buffer.from(v, 'utf-8').toString('base64');
    return Buffer.from(v).toString('base64');
  }
  write(content: string | Uint8Array): void {
    files.set(this.uri, content);
  }
  async copy(dest: MockFile | MockDirectory): Promise<void> {
    const v = files.get(this.uri);
    if (v === undefined) throw new Error('file does not exist');
    const destUri = 'uri' in dest ? dest.uri : this.uri;
    files.set(destUri, v);
  }
  async move(dest: MockFile | MockDirectory): Promise<void> {
    const v = files.get(this.uri);
    if (v === undefined) throw new Error('file does not exist');
    const destUri = 'uri' in dest ? dest.uri : this.uri;
    files.set(destUri, v);
    files.delete(this.uri);
  }
}

class MockDirectory {
  constructor(...uris: (string | MockFile | MockDirectory)[]) {
    const parts = uris.map((u) => (typeof u === 'string' ? u.replace(/\/+$/, '') : u.uri));
    const joined = parts.length > 1 ? parts.join('/').replace(/([^:])\/+/g, '$1/') : (parts[0] ?? '');
    this.uri = joined.endsWith('/') ? joined : joined + '/';
  }
  readonly uri: string;
  get exists(): boolean {
    return dirs.has(this.uri) || this.uri === '/mock/document/' || this.uri === '/mock/cache/';
  }
  create(_options?: { intermediates?: boolean; idempotent?: boolean }): void {
    if (!this.exists) {
      dirs.add(this.uri);
    }
  }
  delete(): void {
    dirs.delete(this.uri);
  }
}

const Paths = {
  document: new MockDirectory('/mock/document/'),
  cache: new MockDirectory('/mock/cache/'),
  bundle: new MockDirectory('/mock/bundle/'),
};

export const File = MockFile;
export const Directory = MockDirectory;
export { Paths };

export function resetFileSystem(): void {
  files.clear();
  dirs.clear();
  dirs.add('/mock/document/');
  dirs.add('/mock/cache/');
}
