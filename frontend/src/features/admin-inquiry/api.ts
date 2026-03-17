import { api } from "@/shared/api";

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

export function fetchAdminInquiries(token: string): Promise<AdminInquiryResponse[]> {
  return api<AdminInquiryResponse[]>("/admin/inquiries", {
    headers: { Authorization: `Bearer ${token}` },
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
