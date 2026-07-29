import {
  getPublicNotice,
  listPublicNotices,
  type NoticeDetailResponse,
  type NoticeListResponse,
} from "@/generated/api/notice";

export function fetchNotices(): Promise<NoticeListResponse[]> {
  return listPublicNotices();
}

export function fetchNotice(id: number): Promise<NoticeDetailResponse> {
  return getPublicNotice(id);
}
