import { api } from "@/shared/api";
import type { CursorPage } from "@/shared/types";

export interface AdminInquiryResponse {
  id: number;
  userId: number;
  userName: string;
  title: string;
  content: string;
  replyContent: string | null;
  repliedAt: string | null;
  createdAt: string;
}

export function fetchAdminInquiries(
  token: string,
  cursor?: string,
): Promise<CursorPage<AdminInquiryResponse>> {
  return api<CursorPage<AdminInquiryResponse>>("/admin/inquiries", {
    headers: { Authorization: `Bearer ${token}` },
    params: { cursor, size: "20" },
  });
}

export function replyInquiry(
  inquiryId: number,
  replyContent: string,
  token: string
): Promise<AdminInquiryResponse> {
  return api<AdminInquiryResponse>(`/admin/inquiries/${inquiryId}/reply`, {
    method: "POST",
    body: { replyContent },
    headers: { Authorization: `Bearer ${token}` },
  });
}
