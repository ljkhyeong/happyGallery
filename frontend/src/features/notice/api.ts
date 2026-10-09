import {
  getPublicNotice,
  listPublicNotices,
  type NoticeDetailResponse,
  type NoticeListResponse,
} from "@/generated/api/notice";
import { queryKeys } from "@/shared/api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";

export function fetchNotices(): Promise<NoticeListResponse[]> {
  return listPublicNotices();
}

export function fetchNotice(id: number): Promise<NoticeDetailResponse> {
  return getPublicNotice(id);
}

/** 홈 공지 목록과 공지 상세의 다른 공지가 같은 캐시를 쓴다. */
export function publicNoticesQuery() {
  return {
    queryKey: queryKeys.notices.all,
    queryFn: fetchNotices,
    staleTime: PUBLIC_DATA_STALE_TIME,
  };
}
