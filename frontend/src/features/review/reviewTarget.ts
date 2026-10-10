export type ReviewTargetType = "PRODUCT" | "CLASS";

/** 후기·찜 대상(작품·클래스)의 상세 화면 경로. */
export function reviewTargetHref(targetType: ReviewTargetType, targetId: number): string {
  return `${targetType === "PRODUCT" ? "/products" : "/classes"}/${targetId}`;
}
