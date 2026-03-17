export interface ProductQnaListItem {
  id: number;
  title: string;
  authorName: string;
  secret: boolean;
  hasReply: boolean;
  createdAt: string;
}

export interface ProductQnaDetail {
  id: number;
  productId: number;
  title: string;
  content: string;
  replyContent: string | null;
  repliedAt: string | null;
  secret: boolean;
  authorName: string;
  createdAt: string;
}

export interface CreateQnaRequest {
  title: string;
  content: string;
  secret: boolean;
  password?: string;
}
