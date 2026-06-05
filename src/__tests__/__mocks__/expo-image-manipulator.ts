export const SaveFormat = {
  JPEG: 'jpeg',
  PNG: 'png',
  WEBP: 'webp',
};

export async function manipulateAsync(
  uri: string,
  _actions: unknown[],
  _options: { compress?: number; format?: string },
): Promise<{ uri: string; width: number; height: number }> {
  return { uri, width: 400, height: 400 };
}
