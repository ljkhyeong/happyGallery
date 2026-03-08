import { api } from "@/shared/api";
import type { SlotResponse, CreateSlotRequest } from "@/shared/types";

export function createSlot(adminKey: string, body: CreateSlotRequest): Promise<SlotResponse> {
  return api<SlotResponse>("/admin/slots", {
    method: "POST",
    headers: { "X-Admin-Key": adminKey },
    body,
  });
}

export function deactivateSlot(adminKey: string, slotId: number): Promise<SlotResponse> {
  return api<SlotResponse>(`/admin/slots/${slotId}/deactivate`, {
    method: "PATCH",
    headers: { "X-Admin-Key": adminKey },
  });
}
