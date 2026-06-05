import { Directory, File, Paths } from 'expo-file-system';
import { Image } from 'react-native';

export interface OptimizedImage {
  readonly uri: string;
  readonly width: number;
  readonly height: number;
  readonly size: number;
}

function getProductDir(): Directory {
  return new Directory(Paths.document, 'productos');
}

function uuid(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

export async function saveProductImage(sourceUri: string): Promise<OptimizedImage> {
  const dir = getProductDir();
  if (!dir.exists) {
    dir.create({ intermediates: true, idempotent: true });
  }

  const target = new File(dir, `${uuid()}.jpg`);
  const source = new File(sourceUri);
  await source.copy(target);

  const size = target.size;
  const dimensions = await new Promise<{ width: number; height: number }>((resolve) => {
    Image.getSize(
      target.uri,
      (width, height) => resolve({ width, height }),
      () => resolve({ width: 400, height: 400 }),
    );
  });
  return { uri: target.uri, size, width: dimensions.width, height: dimensions.height };
}

export async function deleteProductImage(uri: string): Promise<void> {
  try {
    const file = new File(uri);
    if (file.exists) {
      file.delete();
    }
  } catch {
    // ignore
  }
}
