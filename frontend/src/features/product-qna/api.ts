import {
  createProductQna,
  getMyProductQna,
  getPublicProductQna,
  listMyProductQnaPage,
  listProductQnaPage,
  type CreateQnaRequest,
  type MyProductQnaPageResponse,
  type ProductQnaDetail,
  type ProductQnaPageResponse,
  type QnaCreatedResponse,
} from "@/generated/api/productQna";

export const PRODUCT_QNA_PAGE_SIZE = 20;

export function fetchProductQnaPage(
  productId: number,
  cursor?: string,
  signal?: AbortSignal,
): Promise<ProductQnaPageResponse> {
  return listProductQnaPage(
    productId,
    { cursor, size: PRODUCT_QNA_PAGE_SIZE },
    { signal },
  );
}

export function createQna(
  productId: number,
  body: CreateQnaRequest,
): Promise<QnaCreatedResponse> {
  return createProductQna(productId, body);
}

export function fetchMyProductQnaPage(
  productId: number,
  cursor?: string,
  signal?: AbortSignal,
): Promise<MyProductQnaPageResponse> {
  return listMyProductQnaPage(
    productId,
    { cursor, size: PRODUCT_QNA_PAGE_SIZE },
    { signal },
  );
}

export function fetchProductQnaDetail(
  productId: number,
  qnaId: number,
  signal?: AbortSignal,
): Promise<ProductQnaDetail> {
  return getPublicProductQna(productId, qnaId, { signal });
}

export function fetchMyProductQnaDetail(
  productId: number,
  qnaId: number,
  signal?: AbortSignal,
): Promise<ProductQnaDetail> {
  return getMyProductQna(productId, qnaId, { signal });
}
