/** 문의, 상품 Q&A, 공지에 공통으로 적용하는 서버 ContentTextPolicy 경계. */
export const CONTENT_TITLE_MAX_LENGTH = 200;
export const CONTENT_BODY_MAX_LENGTH = 16_000;

export function contentLengthLabel(value: string, maxLength: number): string {
  return `${value.length.toLocaleString("ko-KR")} / ${maxLength.toLocaleString("ko-KR")}자`;
}
