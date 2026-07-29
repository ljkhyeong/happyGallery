import {
  uploadImage,
  type ImageUploadResponse,
} from "@/generated/api/adminCatalog";
import { adminHeaders } from "@/shared/api";

export function uploadAdminImage(
  adminKey: string,
  file: File,
): Promise<ImageUploadResponse> {
  return uploadImage({ file }, { headers: adminHeaders(adminKey) });
}
