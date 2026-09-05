/** 문의·상품 Q&A·공지의 글자 수 제한. 서버 ContentTextPolicy와 같은 값을 사용한다. */
export const CONTENT_TITLE_MAX_LENGTH = 200;
export const CONTENT_BODY_MAX_LENGTH = 16_000;

export function contentLengthLabel(value: string, maxLength: number): string {
  return `${value.length.toLocaleString("ko-KR")} / ${maxLength.toLocaleString("ko-KR")}자`;
}
