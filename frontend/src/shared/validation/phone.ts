export const PHONE_REGEX = /^01[0-9]{8,9}$/;

export function normalizePhone(input: string | null | undefined): string {
  return input?.replace(/\D/g, "") ?? "";
}

export function isValidPhone(phone: string): boolean {
  return PHONE_REGEX.test(phone);
}
