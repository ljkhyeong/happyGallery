import { adminHeaders as h, api } from "@/shared/api";
import type {
  ClassResponse,
  ClassStatus,
  CreateClassRequest,
  UpdateClassRequest,
} from "@/shared/types";

export function fetchAdminClasses(adminKey: string): Promise<ClassResponse[]> {
  return api<ClassResponse[]>("/admin/classes", {
    headers: h(adminKey),
  });
}

export function createClass(adminKey: string, body: CreateClassRequest): Promise<ClassResponse> {
  return api<ClassResponse>("/admin/classes", {
    method: "POST",
    headers: h(adminKey),
    body,
  });
}

export function updateClass(
  adminKey: string,
  classId: number,
  body: UpdateClassRequest,
): Promise<ClassResponse> {
  return api<ClassResponse>(`/admin/classes/${classId}`, {
    method: "PATCH",
    headers: h(adminKey),
    body,
  });
}

export function updateClassStatus(
  adminKey: string,
  classId: number,
  status: ClassStatus,
): Promise<ClassResponse> {
  return api<ClassResponse>(`/admin/classes/${classId}/status`, {
    method: "PATCH",
    headers: h(adminKey),
    body: { status },
  });
}
