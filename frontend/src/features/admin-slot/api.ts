import { adminHeaders as h, api } from "@/shared/api";
import type {
  BulkSlotRequest,
  BulkSlotResponse,
  ClassResponse,
  CreateSlotRequest,
  SlotResponse,
} from "@/shared/types";

export function fetchClasses(adminKey: string): Promise<ClassResponse[]> {
  return api<ClassResponse[]>("/admin/classes", {
    headers: h(adminKey),
  });
}

export function fetchSlotsByClass(adminKey: string, classId: number): Promise<SlotResponse[]> {
  return api<SlotResponse[]>("/admin/slots", {
    headers: h(adminKey),
    params: { classId },
  });
}

export function createSlot(adminKey: string, body: CreateSlotRequest): Promise<SlotResponse> {
  return api<SlotResponse>("/admin/slots", {
    method: "POST",
    headers: h(adminKey),
    body,
  });
}

export function previewBulkSlots(
  adminKey: string,
  body: BulkSlotRequest,
): Promise<BulkSlotResponse> {
  return api<BulkSlotResponse>("/admin/slots/bulk/preview", {
    method: "POST",
    headers: h(adminKey),
    body,
  });
}

export function createBulkSlots(
  adminKey: string,
  body: BulkSlotRequest,
): Promise<BulkSlotResponse> {
  return api<BulkSlotResponse>("/admin/slots/bulk", {
    method: "POST",
    headers: h(adminKey),
    body,
  });
}

export function deactivateSlot(adminKey: string, slotId: number): Promise<SlotResponse> {
  return api<SlotResponse>(`/admin/slots/${slotId}/deactivate`, {
    method: "PATCH",
    headers: h(adminKey),
  });
}

export function activateSlot(adminKey: string, slotId: number): Promise<SlotResponse> {
  return api<SlotResponse>(`/admin/slots/${slotId}/activate`, {
    method: "PATCH",
    headers: h(adminKey),
  });
}
