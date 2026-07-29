import {
  activateSlot as activateAdminSlot,
  cancelAdminSlotSession,
  createBulkSlots as createAdminBulkSlots,
  createSlot as createAdminSlot,
  deactivateSlot as deactivateAdminSlot,
  listClasses,
  listSlots,
  previewBulkSlots as previewAdminBulkSlots,
  BulkSlotRequestWeekdaysItem,
  type AdminClassResponse,
  type AdminSlotSessionCancelRequest,
  type AdminSlotSessionCancelResponse,
  type BulkSlotRequest,
  type BulkSlotRequestWeekdaysItem as BulkSlotWeekday,
  type BulkSlotResponse,
  type CreateSlotRequest,
  type SlotResponse,
} from "@/generated/api/adminCatalog";
import { adminHeaders } from "@/shared/api";

type BulkSlotFormRequest = Omit<BulkSlotRequest, "weekdays"> & {
  weekdays: string[];
};

export function fetchClasses(adminKey: string): Promise<AdminClassResponse[]> {
  return listClasses({ headers: adminHeaders(adminKey) });
}

export function cancelSlotSession(
  adminKey: string,
  slotId: number,
  body: AdminSlotSessionCancelRequest,
): Promise<AdminSlotSessionCancelResponse> {
  return cancelAdminSlotSession(slotId, body, { headers: adminHeaders(adminKey) });
}

export function fetchSlotsByClass(adminKey: string, classId: number): Promise<SlotResponse[]> {
  return listSlots({ classId }, { headers: adminHeaders(adminKey) });
}

export function createSlot(
  adminKey: string,
  body: CreateSlotRequest,
): Promise<SlotResponse> {
  return createAdminSlot(body, { headers: adminHeaders(adminKey) });
}

export function previewBulkSlots(
  adminKey: string,
  body: BulkSlotFormRequest,
): Promise<BulkSlotResponse> {
  return previewAdminBulkSlots(
    { ...body, weekdays: bulkSlotWeekdays(body.weekdays) },
    { headers: adminHeaders(adminKey) },
  );
}

export function createBulkSlots(
  adminKey: string,
  body: BulkSlotFormRequest,
): Promise<BulkSlotResponse> {
  return createAdminBulkSlots(
    { ...body, weekdays: bulkSlotWeekdays(body.weekdays) },
    { headers: adminHeaders(adminKey) },
  );
}

export function deactivateSlot(adminKey: string, slotId: number): Promise<SlotResponse> {
  return deactivateAdminSlot(slotId, { headers: adminHeaders(adminKey) });
}

export function activateSlot(adminKey: string, slotId: number): Promise<SlotResponse> {
  return activateAdminSlot(slotId, { headers: adminHeaders(adminKey) });
}

function bulkSlotWeekdays(values: string[]): BulkSlotWeekday[] {
  return values.map((value) => {
    const matched = Object.values(BulkSlotRequestWeekdaysItem)
      .find((candidate) => candidate === value);
    if (matched === undefined) {
      throw new Error("지원하지 않는 운영 요일입니다.");
    }
    return matched;
  });
}
