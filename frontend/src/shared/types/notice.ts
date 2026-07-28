export interface NoticeListItem {
  id: number;
  title: string;
  pinned: boolean;
  viewCount: number;
  version: number;
  createdAt: string;
}

export interface NoticeDetail {
  id: number;
  title: string;
  content: string;
  pinned: boolean;
  viewCount: number;
  version: number;
  createdAt: string;
}

export interface CreateNoticeRequest {
  title: string;
  content: string;
  pinned: boolean;
}

export interface UpdateNoticeRequest extends CreateNoticeRequest {
  expectedVersion: number;
}
