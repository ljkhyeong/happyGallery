/** 작품 카테고리. 관리자가 상품에 입력하는 카테고리 이름과 같아야 목록 필터가 맞는다. */
export const SHOP_CATEGORIES = ["가죽", "레진", "향·아로마", "새활용", "톨페인팅", "플루이드아트"] as const;

export type ShopCategory = (typeof SHOP_CATEGORIES)[number];
