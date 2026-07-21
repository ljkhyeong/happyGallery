import { adminHeaders, api } from "@/shared/api";

export function uploadAdminImage(adminKey: string, file: File): Promise<{ url: string }> {
  const body = new FormData();
  body.append("file", file);
  return api<{ url: string }>("/admin/media/images", {
    method: "POST",
    headers: adminHeaders(adminKey),
    body,
  });
}
