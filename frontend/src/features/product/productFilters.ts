import { ListProductsSort, ListProductsType, type ListProductsParams } from "@/generated/api/product";

/** 서버 첫 조회와 브라우저가 같은 상품 검색 조건을 사용한다. */
export function readProductFilters(params: URLSearchParams): ListProductsParams {
  const type = Object.values(ListProductsType).find((value) => value === params.get("type"));
  const sort = Object.values(ListProductsSort).find((value) => value === params.get("sort"));
  const category = params.get("category")?.trim();
  const keyword = params.get("keyword")?.trim().slice(0, 100);
  return {
    ...(type && { type }),
    ...(category && category !== "ALL" && { category }),
    ...(keyword && { keyword }),
    ...(sort && sort !== "newest" && { sort }),
  };
}
