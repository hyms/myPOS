import * as ImageManipulator from 'expo-image-manipulator';
import { SaveFormat } from 'expo-image-manipulator';

import { saveProductImage } from './ImageStorage';
import type { OptimizedImage } from './ImageStorage';

export async function optimizeProductImage(sourceUri: string): Promise<OptimizedImage> {
  const manipulated = await ImageManipulator.manipulateAsync(
    sourceUri,
    [{ resize: { width: 400, height: 400 } }],
    {
      compress: 0.7,
      format: SaveFormat.JPEG,
    },
  );
  return saveProductImage(manipulated.uri);
}
