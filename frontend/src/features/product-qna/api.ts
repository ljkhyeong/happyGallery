import {
  createProductQna,
  getPublicProductQna,
  listProductQna,
  verifyProductQnaPassword,
  type CreateQnaRequest,
  type ProductQnaDetail,
  type ProductQnaListItem,
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

export function fetchProductQnaDetail(productId: number, qnaId: number): Promise<ProductQnaDetail> {
  return getPublicProductQna(productId, qnaId);
}

export function verifyQnaPassword(productId: number, qnaId: number, password: string): Promise<ProductQnaDetail> {
  return verifyProductQnaPassword(productId, qnaId, { password });
}
