import {
  listAdminInquiries,
  replyToAdminInquiry,
  type AdminInquiryPageResponse,
  type AdminInquiryResponse,
} from "@/generated/api/adminOperations";
import { adminHeaders } from "@/shared/api";

export type { AdminInquiryResponse } from "@/generated/api/adminOperations";

export function fetchAdminInquiries(
  token: string,
  cursor?: string,
): Promise<AdminInquiryPageResponse> {
  return listAdminInquiries(
    { cursor, size: 20 },
    { headers: adminHeaders(token) },
  );
}

export function replyInquiry(
  inquiryId: number,
  replyContent: string,
  token: string,
): Promise<AdminInquiryResponse> {
  return replyToAdminInquiry(
    inquiryId,
    { replyContent },
    { headers: adminHeaders(token) },
  );
}
