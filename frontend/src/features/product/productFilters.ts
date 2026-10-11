import { ListProductsSort, ListProductsType, type ListProductsParams } from "@/generated/api/product";

const PRODUCT_KEYWORD_MAX_LENGTH = 100;

/** 작품 이름 검색어는 앞뒤 공백을 지우고 서버 검색 길이까지만 보낸다. */
export function normalizeProductKeyword(value: string): string {
  return value.trim().slice(0, PRODUCT_KEYWORD_MAX_LENGTH);
}

/**
 * 작품 목록 제목·한 줄 안내·문서 제목 이름. 머리글 카테고리·주문 제작·검색으로 들어오면 그 조건을
 * 제목으로 보여 줘 전체 목록과 구분한다. 서버 메타와 화면 제목이 같은 값을 쓴다.
 */
export function productListHeading(filters: ListProductsParams): {
  title: string;
  lead?: string;
  documentName?: string;
} {
  if (filters.keyword) return { title: `‘${filters.keyword}’ 검색 결과`, documentName: `‘${filters.keyword}’ 검색 결과` };
  if (filters.category) return { title: filters.category, documentName: `${filters.category} 작품` };
  if (filters.type === ListProductsType.MADE_TO_ORDER) {
    return {
      title: "주문 제작",
      lead: "주문을 받은 뒤 공방에서 만듭니다. 제작 기간은 작품마다 다릅니다.",
      documentName: "주문 제작 작품",
    };
  }
  if (filters.type === ListProductsType.READY_STOCK) {
    return {
      title: "바로 구매",
      lead: "만들어 둔 작품이라 주문 뒤 바로 준비합니다.",
      documentName: "바로 구매할 수 있는 작품",
    };
  }
  return { title: "전체 작품" };
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
