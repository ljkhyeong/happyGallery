import {
  createMyInquiry,
  listMyInquiriesPage,
  type CreateInquiryRequest,
  type InquiryResponse,
  type MyInquiryPageResponse,
} from "@/generated/api/customerStore";

export function fetchMyInquiriesPage(
  cursor?: string,
  signal?: AbortSignal,
): Promise<MyInquiryPageResponse> {
  return listMyInquiriesPage({ cursor, size: 20 }, { signal });
}

export function createInquiry(body: CreateInquiryRequest): Promise<InquiryResponse> {
  return createMyInquiry(body);
}
