import {
  getAdminWorkshopProfile,
  getWorkshopProfile,
  updateAdminWorkshopProfile,
} from "@/generated/api/workshop";
import { adminHeaders } from "@/shared/api";
import type { UpdateWorkshopProfileRequest, WorkshopProfile } from "@/shared/types";

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
