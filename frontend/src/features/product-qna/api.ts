import { api } from "@/shared/api";
import type { ProductQnaListItem, ProductQnaDetail, CreateQnaRequest } from "@/shared/types";

export function fetchProductQna(productId: number): Promise<ProductQnaListItem[]> {
  return api<ProductQnaListItem[]>(`/products/${productId}/qna`);
}

export function createQna(productId: number, body: CreateQnaRequest): Promise<{ id: number }> {
  return api(`/me/products/${productId}/qna`, { method: "POST", body });
}

export function verifyQnaPassword(productId: number, qnaId: number, password: string): Promise<ProductQnaDetail> {
  return api<ProductQnaDetail>(`/products/${productId}/qna/${qnaId}/verify`, {
    method: "POST",
    body: { password },
  });
}
