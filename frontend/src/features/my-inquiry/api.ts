import {
  createMyInquiry,
  listMyInquiries,
  type CreateInquiryRequest,
  type InquiryResponse,
} from "@/generated/api/customerStore";

export function fetchMyInquiries(): Promise<InquiryResponse[]> {
  return listMyInquiries();
}

export function createInquiry(body: CreateInquiryRequest): Promise<InquiryResponse> {
  return createMyInquiry(body);
}
