import {
  changeAdminClassStatus,
  createClass as createAdminClass,
  listClasses,
  updateClass as updateAdminClass,
  type AdminClassResponse,
  type CreateClassRequest,
  type UpdateClassRequest,
  type UpdateClassStatusRequestStatus,
} from "@/generated/api/adminCatalog";
import { adminHeaders } from "@/shared/api";

export type ClassStatus = UpdateClassStatusRequestStatus;

export function fetchAdminClasses(adminKey: string): Promise<AdminClassResponse[]> {
  return listClasses({ headers: adminHeaders(adminKey) });
}

export function createClass(
  adminKey: string,
  body: CreateClassRequest,
): Promise<AdminClassResponse> {
  return createAdminClass(body, { headers: adminHeaders(adminKey) });
}

export function updateClass(
  adminKey: string,
  classId: number,
  body: UpdateClassRequest,
): Promise<AdminClassResponse> {
  return updateAdminClass(classId, body, { headers: adminHeaders(adminKey) });
}

export function updateClassStatus(
  adminKey: string,
  classId: number,
  status: ClassStatus,
): Promise<AdminClassResponse> {
  return changeAdminClassStatus(
    classId,
    { status },
    { headers: adminHeaders(adminKey) },
  );
}
