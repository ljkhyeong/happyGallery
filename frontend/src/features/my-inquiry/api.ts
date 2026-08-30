import {
  createMyInquiry,
  listMyInquiries as requestRecentMyInquiries,
  listMyInquiriesPage,
  type CreateInquiryRequest,
  type InquiryResponse,
  type MyInquiryPageResponse,
} from "@/generated/api/customerStore";

export function fetchRecentMyInquiries(signal?: AbortSignal): Promise<InquiryResponse[]> {
  return requestRecentMyInquiries({ signal });
}

export function fetchMyInquiriesPage(
  cursor?: string,
  signal?: AbortSignal,
): Promise<MyInquiryPageResponse> {
  return listMyInquiriesPage({ cursor, size: 20 }, { signal });
}

export function createInquiry(body: CreateInquiryRequest): Promise<InquiryResponse> {
  return createMyInquiry(body);
}
