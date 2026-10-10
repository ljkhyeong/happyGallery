import { ListProductsSort, ListProductsType, type ListProductsParams } from "@/generated/api/product";

const PRODUCT_KEYWORD_MAX_LENGTH = 100;

/** 작품 이름 검색어는 앞뒤 공백을 지우고 서버 검색 길이까지만 보낸다. */
export function normalizeProductKeyword(value: string): string {
  return value.trim().slice(0, PRODUCT_KEYWORD_MAX_LENGTH);
}

/** 서버 첫 조회와 브라우저가 같은 상품 검색 조건을 사용한다. */
export function readProductFilters(params: URLSearchParams): ListProductsParams {
  const type = Object.values(ListProductsType).find((value) => value === params.get("type"));
  const sort = Object.values(ListProductsSort).find((value) => value === params.get("sort"));
  const category = params.get("category")?.trim();
  const keyword = normalizeProductKeyword(params.get("keyword") ?? "");
  return {
    ...(type && { type }),
    ...(category && category !== "ALL" && { category }),
    ...(keyword && { keyword }),
    ...(sort && sort !== "newest" && { sort }),
  };
}
