/** 주소·선택 상자에서 받은 문자열이 생성된 enum 값일 때만 그 값을 돌려준다. */
export function parseEnumValue<T extends string>(
  values: Readonly<Record<string, T>>,
  raw: string | null | undefined,
): T | undefined {
  return Object.values(values).find((value) => value === raw);
}
