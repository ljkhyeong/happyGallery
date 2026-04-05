import { adminHeaders as h, api } from "@/shared/api";
import type { SlotResponse, CreateSlotRequest, ClassResponse } from "@/shared/types";

export function fetchClasses(): Promise<ClassResponse[]> {
  return api<ClassResponse[]>("/classes");
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

export function deactivateSlot(adminKey: string, slotId: number): Promise<SlotResponse> {
  return api<SlotResponse>(`/admin/slots/${slotId}/deactivate`, {
    method: "PATCH",
    headers: h(adminKey),
  });
}
