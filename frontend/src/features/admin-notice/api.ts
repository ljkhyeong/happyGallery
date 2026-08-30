import {
  createAdminNotice,
  deleteAdminNotice,
  getAdminNotice,
  listAdminNotices,
  updateAdminNotice,
  CreateNoticeRequest,
  NoticeDetailResponse,
  NoticeListResponse,
  UpdateNoticeRequest,
} from "@/generated/api/adminNotice";
import { adminHeaders } from "@/shared/api";

export type {
  CreateNoticeRequest,
  NoticeDetailResponse,
  NoticeListResponse,
  UpdateNoticeRequest,
} from "@/generated/api/adminNotice";

export function fetchAdminNotices(token: string): Promise<NoticeListResponse[]> {
  return listAdminNotices({
    headers: adminHeaders(token),
  });
}

export function fetchAdminNotice(id: number, token: string): Promise<NoticeDetailResponse> {
  return getAdminNotice(id, {
    headers: adminHeaders(token),
  });
}

export function createNotice(
  req: CreateNoticeRequest,
  token: string,
): Promise<NoticeDetailResponse> {
  return createAdminNotice(req, {
    headers: adminHeaders(token),
  });
}

export function updateNotice(
  id: number,
  req: UpdateNoticeRequest,
  token: string,
): Promise<NoticeDetailResponse> {
  return updateAdminNotice(id, req, {
    headers: adminHeaders(token),
  });
}

export function deleteNotice(id: number, expectedVersion: number, token: string): Promise<void> {
  return deleteAdminNotice(id, { expectedVersion }, {
    headers: adminHeaders(token),
  });
}
