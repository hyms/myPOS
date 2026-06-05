let result: { canceled?: boolean; assets?: { uri: string }[] } | null = null;

export function mockDocumentPickerResult(mock: {
  canceled?: boolean;
  assets?: { uri: string }[];
}): void {
  result = mock;
}

export function resetDocumentPicker(): void {
  result = null;
}

export async function getDocumentAsync(
  _options?: Record<string, unknown>,
): Promise<{ canceled?: boolean; assets?: { uri: string }[] }> {
  if (result) return result;
  return { canceled: true };
}
