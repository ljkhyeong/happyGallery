export const PHONE_REGEX = /^01[0-9]{8,9}$/;

/** 숫자만 남긴다. 자동완성·복사로 들어온 국가번호 형식(+82 10-…)은 국내 형식(010…)으로 바꾼다. */
export function normalizePhone(input: string | null | undefined): string {
  const digits = input?.replace(/\D/g, "") ?? "";
  return /^821[0-9]{8,9}$/.test(digits) ? `0${digits.slice(2)}` : digits;
}

/**
 * 입력 칸용 값. maxLength로 막으면 "010-1234-5678" 같은 자동완성 값이 잘리므로
 * 하이픈·공백을 먼저 지운 뒤 11자리까지만 남긴다.
 */
export function toPhoneInputValue(input: string): string {
  return normalizePhone(input).slice(0, 11);
}

/**
 * 붙여넣은 글에서 읽은 휴대폰 번호. 숫자가 11자리를 넘으면 번호가 아닌 글로 보고 null을 돌려
 * 브라우저 기본 붙여넣기에 맡긴다.
 */
export function pastedPhoneValue(text: string): string | null {
  const phone = normalizePhone(text);
  return phone.length <= 11 ? phone : null;
}

export function isValidPhone(phone: string): boolean {
  return PHONE_REGEX.test(phone);
}
