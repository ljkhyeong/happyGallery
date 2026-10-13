/** 주문·예약 상세의 후기 구역 id. 후기 작성 바로가기와 후기 요청 알림이 `#review`로 연다. */
export const REVIEW_SECTION_ID = "review";

export function reviewSectionHref(detailPath: string): string {
  return `${detailPath}#${REVIEW_SECTION_ID}`;
}
