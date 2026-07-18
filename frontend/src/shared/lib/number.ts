const POSITIVE_INTEGER_PATTERN = /^[1-9]\d*$/;

export function isPositiveSafeIntegerString(value: string | null | undefined): boolean {
  const candidate = value ?? "";
  return POSITIVE_INTEGER_PATTERN.test(candidate)
    && Number.isSafeInteger(Number(candidate));
}
