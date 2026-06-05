let hasHardware = true;
let isEnrolled = true;
let authSuccess = true;

export function mockHardware(available: boolean): void {
  hasHardware = available;
}

export function mockEnrolled(enrolled: boolean): void {
  isEnrolled = enrolled;
}

export function mockAuthSuccess(success: boolean): void {
  authSuccess = success;
}

export async function hasHardwareAsync(): Promise<boolean> {
  return hasHardware;
}

export async function isEnrolledAsync(): Promise<boolean> {
  return isEnrolled;
}

export async function authenticateAsync(
  _options: Record<string, unknown>,
): Promise<{ success: boolean; error?: string }> {
  return { success: authSuccess };
}
