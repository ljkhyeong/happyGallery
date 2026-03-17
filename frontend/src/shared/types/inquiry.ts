export interface InquiryResponse {
  id: number;
  title: string;
  content: string;
  hasReply: boolean;
  replyContent: string | null;
  repliedAt: string | null;
  createdAt: string;
}

export interface CreateInquiryRequest {
  title: string;
  content: string;
}
