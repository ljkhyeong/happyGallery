import { api } from "@/shared/api";
import type { NoticeListItem, NoticeDetail } from "@/shared/types";

export function fetchNotices(): Promise<NoticeListItem[]> {
  return api<NoticeListItem[]>("/notices");
}

export function fetchNotice(id: number): Promise<NoticeDetail> {
  return api<NoticeDetail>(`/notices/${id}`);
}
