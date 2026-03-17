import { api } from "@/shared/api";
import type { InquiryResponse, CreateInquiryRequest } from "@/shared/types";

export function fetchMyInquiries(): Promise<InquiryResponse[]> {
  return api<InquiryResponse[]>("/me/inquiries");
}

export function fetchMyInquiry(id: number): Promise<InquiryResponse> {
  return api<InquiryResponse>(`/me/inquiries/${id}`);
}

export function createInquiry(body: CreateInquiryRequest): Promise<InquiryResponse> {
  return api<InquiryResponse>("/me/inquiries", { method: "POST", body });
}
