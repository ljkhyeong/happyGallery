import { adminHeaders as h, api } from "@/shared/api";
import type { ClassResponse, CreateClassRequest } from "@/shared/types";

export function createClass(adminKey: string, body: CreateClassRequest): Promise<ClassResponse> {
  return api<ClassResponse>("/admin/classes", {
    method: "POST",
    headers: h(adminKey),
    body,
  });
}
