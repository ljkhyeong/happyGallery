import { api } from "@/shared/api";

export interface AdminQnaResponse {
  id: number;
  productId: number;
  userId: number;
  authorName: string;
  title: string;
  content: string;
  secret: boolean;
  replyContent: string | null;
  repliedAt: string | null;
  createdAt: string;
}

export function fetchAdminQna(productId: number, token: string): Promise<AdminQnaResponse[]> {
  return api<AdminQnaResponse[]>("/admin/qna", {
    params: { productId },
    headers: { Authorization: `Bearer ${token}` },
  });
}

export function replyQna(
  qnaId: number,
  replyContent: string,
  token: string
): Promise<AdminQnaResponse> {
  return api<AdminQnaResponse>(`/admin/qna/${qnaId}/reply`, {
    method: "POST",
    body: { replyContent },
    headers: { Authorization: `Bearer ${token}` },
  });
}
