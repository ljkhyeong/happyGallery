import {
  answerSmartStoreInquiry,
  answerSmartStoreCustomerInquiry,
  updateSmartStoreCustomerInquiryAnswer,
  getSmartStoreInquiryAnswerTemplate,
  listAdminProductQnaPage,
  listSmartStoreInquiries,
  listSmartStoreCustomerInquiries,
  listUnansweredAdminProductQna,
  replyProductQna,
  type AdminQnaPageResponse,
  type AdminQnaResponse,
  type SmartStoreInquiryResponse,
  type SmartStoreCustomerInquiryResponse,
  type SmartStoreInquiryAnswerTemplateResponse,
} from "@/generated/api/productQna";
import { adminHeaders } from "@/shared/api";

export type { AdminQnaResponse } from "@/generated/api/productQna";
export type { SmartStoreInquiryResponse } from "@/generated/api/productQna";
export type { SmartStoreCustomerInquiryResponse } from "@/generated/api/productQna";
export type { SmartStoreInquiryAnswerTemplateResponse } from "@/generated/api/productQna";

export function fetchSmartStoreInquiries(
  token: string,
  unansweredOnly: boolean,
): Promise<SmartStoreInquiryResponse[]> {
  return listSmartStoreInquiries({ unansweredOnly, limit: 100 }, {
    headers: adminHeaders(token),
  });
}

export function fetchSmartStoreAnswerTemplate(
  token: string,
): Promise<SmartStoreInquiryAnswerTemplateResponse> {
  return getSmartStoreInquiryAnswerTemplate({
    headers: adminHeaders(token),
  });
}

export function answerChannelQna(
  questionId: number,
  content: string,
  token: string,
): Promise<void> {
  return answerSmartStoreInquiry(questionId, { content }, {
    headers: adminHeaders(token),
  });
}

export function fetchSmartStoreCustomerInquiries(
  token: string,
  unansweredOnly: boolean,
): Promise<SmartStoreCustomerInquiryResponse[]> {
  return listSmartStoreCustomerInquiries({ unansweredOnly, limit: 100 }, {
    headers: adminHeaders(token),
  });
}

export function answerCustomerInquiry(
  inquiryNo: number,
  content: string,
  token: string,
): Promise<void> {
  return answerSmartStoreCustomerInquiry(inquiryNo, { content }, {
    headers: adminHeaders(token),
  });
}

export function updateCustomerInquiryAnswer(
  inquiryNo: number,
  answerContentId: number,
  content: string,
  token: string,
): Promise<void> {
  return updateSmartStoreCustomerInquiryAnswer(inquiryNo, answerContentId, { content }, {
    headers: adminHeaders(token),
  });
}

export function fetchAdminQnaPage(
  productId: number,
  token: string,
  cursor?: string,
): Promise<AdminQnaPageResponse> {
  return listAdminProductQnaPage({ productId, cursor, size: 20 }, {
    headers: adminHeaders(token),
  });
}

export function fetchUnansweredAdminQna(
  token: string,
  cursor?: string,
): Promise<AdminQnaPageResponse> {
  return listUnansweredAdminProductQna({ cursor, size: 20 }, {
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
