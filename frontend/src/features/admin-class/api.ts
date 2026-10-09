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
import { queryOptions } from "@tanstack/react-query";
import { adminHeaders, queryKeys } from "@/shared/api";

export type ClassStatus = UpdateClassStatusRequestStatus;

export function fetchAdminClasses(adminKey: string): Promise<AdminClassResponse[]> {
  return listClasses({ headers: adminHeaders(adminKey) });
}

/** 클래스 관리·회차 관리·직접 예약 등록이 같은 캐시로 공유하는 클래스 목록 조회 */
export function adminClassesQuery(adminKey: string) {
  return queryOptions({
    queryKey: queryKeys.admin.classes,
    queryFn: () => fetchAdminClasses(adminKey),
  });
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
