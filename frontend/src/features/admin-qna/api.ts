import {
  listAdminProductQna,
  replyProductQna,
  type AdminQnaResponse,
} from "@/generated/api/productQna";
import { adminHeaders } from "@/shared/api";

export type { AdminQnaResponse } from "@/generated/api/productQna";

export function fetchAdminQna(productId: number, token: string): Promise<AdminQnaResponse[]> {
  return listAdminProductQna({ productId }, {
    headers: adminHeaders(token),
  });
}

export function replyQna(
  qnaId: number,
  replyContent: string,
  token: string,
): Promise<AdminQnaResponse> {
  return replyProductQna(qnaId, { replyContent }, {
    headers: adminHeaders(token),
  });
}
