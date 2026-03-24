export interface NoticeListItem {
  id: number;
  title: string;
  pinned: boolean;
  viewCount: number;
  createdAt: string;
}

export interface NoticeDetail {
  id: number;
  title: string;
  content: string;
  pinned: boolean;
  viewCount: number;
  createdAt: string;
}

export interface CreateNoticeRequest {
  title: string;
  content: string;
  pinned: boolean;
}
