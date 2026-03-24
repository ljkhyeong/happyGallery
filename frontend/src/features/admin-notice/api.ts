import { api } from "@/shared/api";
import type { NoticeListItem, NoticeDetail, CreateNoticeRequest } from "@/shared/types";

export function fetchAdminNotices(token: string): Promise<NoticeListItem[]> {
  return api<NoticeListItem[]>("/admin/notices", {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export function createNotice(req: CreateNoticeRequest, token: string): Promise<NoticeDetail> {
  return api<NoticeDetail>("/admin/notices", {
    method: "POST",
    body: req,
    headers: { Authorization: `Bearer ${token}` },
  });
}

export function updateNotice(id: number, req: CreateNoticeRequest, token: string): Promise<NoticeDetail> {
  return api<NoticeDetail>(`/admin/notices/${id}`, {
    method: "PUT",
    body: req,
    headers: { Authorization: `Bearer ${token}` },
  });
}

export function deleteNotice(id: number, token: string): Promise<void> {
  return api<void>(`/admin/notices/${id}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });
}
