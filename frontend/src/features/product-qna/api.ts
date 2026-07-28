import {
  createProductQna,
  getMyProductQna,
  getPublicProductQna,
  listMyProductQna,
  listProductQna,
  type CreateQnaRequest,
  type ProductQnaDetail,
  type ProductQnaListItem,
  type MyProductQnaListItem,
  type QnaCreatedResponse,
} from "@/generated/api/productQna";

export function fetchProductQna(productId: number): Promise<ProductQnaListItem[]> {
  return listProductQna(productId);
}

export function createQna(
  productId: number,
  body: CreateQnaRequest,
): Promise<QnaCreatedResponse> {
  return createProductQna(productId, body);
}

export function fetchMyProductQna(productId: number): Promise<MyProductQnaListItem[]> {
  return listMyProductQna(productId);
}

export function fetchProductQnaDetail(productId: number, qnaId: number): Promise<ProductQnaDetail> {
  return getPublicProductQna(productId, qnaId);
}

export function fetchMyProductQnaDetail(productId: number, qnaId: number): Promise<ProductQnaDetail> {
  return getMyProductQna(productId, qnaId);
}
