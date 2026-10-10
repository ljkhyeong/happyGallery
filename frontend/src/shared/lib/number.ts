const POSITIVE_INTEGER_PATTERN = /^[1-9]\d*$/;

export function isPositiveSafeIntegerString(value: string | null | undefined): boolean {
  const candidate = value ?? "";
  return POSITIVE_INTEGER_PATTERN.test(candidate)
    && Number.isSafeInteger(Number(candidate));
}

/** 주소·입력값의 ID를 `Number`로 읽어 1 이상의 안전한 정수일 때만 돌려준다. */
export function parsePositiveId(value: string | null | undefined): number | null {
  const id = Number(value);
  return Number.isSafeInteger(id) && id > 0 ? id : null;
}
