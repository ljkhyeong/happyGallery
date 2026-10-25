import {
  getAdminWorkshopProfile,
  getWorkshopProfile,
  updateAdminWorkshopProfile,
  type UpdateWorkshopProfileRequest,
} from "@/generated/api/workshop";
import { adminHeaders } from "@/shared/api";
import type { WorkshopProfile } from "@/shared/types";

export function fetchWorkshopProfile(): Promise<WorkshopProfile> {
  return getWorkshopProfile();
}

export function fetchAdminWorkshopProfile(adminKey: string): Promise<WorkshopProfile> {
  return getAdminWorkshopProfile({ headers: adminHeaders(adminKey) });
}

export function updateWorkshopProfile(
  adminKey: string,
  body: UpdateWorkshopProfileRequest,
): Promise<WorkshopProfile> {
  return updateAdminWorkshopProfile(body, {
    headers: adminHeaders(adminKey),
  });
}
