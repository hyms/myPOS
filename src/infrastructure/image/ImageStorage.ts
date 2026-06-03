import * as FileSystem from 'expo-file-system/legacy';
import { Image } from 'react-native';

export interface OptimizedImage {
  readonly uri: string;
  readonly width: number;
  readonly height: number;
  readonly size: number;
}

const PRODUCT_DIR = `${FileSystem.documentDirectory}productos/`;

async function ensureDir(): Promise<void> {
  const info = await FileSystem.getInfoAsync(PRODUCT_DIR);
  if (!info.exists) {
    await FileSystem.makeDirectoryAsync(PRODUCT_DIR, { intermediates: true });
  }
}

function uuid(): string {
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

export async function saveProductImage(sourceUri: string): Promise<OptimizedImage> {
  await ensureDir();
  const target = `${PRODUCT_DIR}${uuid()}.jpg`;
  await FileSystem.copyAsync({ from: sourceUri, to: target });
  const info = await FileSystem.getInfoAsync(target, { size: true });
  const size = info.exists && 'size' in info && info.size ? info.size : 0;
  const dimensions = await new Promise<{ width: number; height: number }>((resolve) => {
    Image.getSize(
      target,
      (width, height) => resolve({ width, height }),
      () => resolve({ width: 400, height: 400 }),
    );
  });
  return { uri: target, size, width: dimensions.width, height: dimensions.height };
}

export async function deleteProductImage(uri: string): Promise<void> {
  try {
    await FileSystem.deleteAsync(uri, { idempotent: true });
  } catch {
    // ignore
  }
}
