export const PASSWORD_MAX_UTF8_BYTES = 72;

export function isPasswordWithinByteLimit(value: string): boolean {
  return new TextEncoder().encode(value).length <= PASSWORD_MAX_UTF8_BYTES;
}
