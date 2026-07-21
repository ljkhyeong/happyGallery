import { api, adminHeaders } from "@/shared/api";
import type { UpdateWorkshopProfileRequest, WorkshopProfile } from "@/shared/types";

export function fetchWorkshopProfile(): Promise<WorkshopProfile> {
  return api<WorkshopProfile>("/workshop");
}

export function fetchAdminWorkshopProfile(adminKey: string): Promise<WorkshopProfile> {
  return api<WorkshopProfile>("/admin/workshop", { headers: adminHeaders(adminKey) });
}

export function updateWorkshopProfile(
  adminKey: string,
  body: UpdateWorkshopProfileRequest,
): Promise<WorkshopProfile> {
  return api<WorkshopProfile>("/admin/workshop", {
    method: "PUT",
    headers: adminHeaders(adminKey),
    body,
  });
}
