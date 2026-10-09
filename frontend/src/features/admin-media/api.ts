import {
  getAdminImage,
  uploadImage,
  type ImageUploadResponse,
} from "@/generated/api/adminCatalog";
import { adminHeaders } from "@/shared/api";
import { assertImageBlob } from "@/shared/hooks/useBlobObjectUrl";

const PUBLIC_IMAGE_PREFIX = "/api/v1/media/images/";

export function uploadAdminImage(
  adminKey: string,
  file: File,
): Promise<ImageUploadResponse> {
  return uploadImage({ file }, { headers: adminHeaders(adminKey) });
}

export function localAdminImageFileName(imageUrl: string): string | undefined {
  const url = new URL(imageUrl, window.location.origin);
  if (url.origin !== window.location.origin || !url.pathname.startsWith(PUBLIC_IMAGE_PREFIX)) {
    return undefined;
  }
  const fileName = url.pathname.slice(PUBLIC_IMAGE_PREFIX.length);
  return fileName && !fileName.includes("/") ? fileName : undefined;
}

export async function fetchAdminImagePreview(
  adminKey: string,
  fileName: string,
  signal?: AbortSignal,
): Promise<Blob> {
  const blob = await getAdminImage(fileName, {
    headers: adminHeaders(adminKey),
    cache: "no-store",
    signal,
  });
  return assertImageBlob(blob, "대표 이미지 응답 형식이 올바르지 않습니다.");
}
