import {
  activateSlot as activateAdminSlot,
  cancelAdminSlotSession,
  createAdminBookingTimeBlock,
  deleteAdminBookingTimeBlock,
  deactivateSlot as deactivateAdminSlot,
  getAdminBookingCalendar,
  listClasses,
  listSlots,
  updateAdminBookingCalendarDay,
  updateAdminBookingCalendarSettings,
  type AdminClassResponse,
  type AdminSlotSessionCancelRequest,
  type AdminSlotSessionCancelResponse,
  type BookingCalendarResponse,
  type BookingCalendarSettingsResponse,
  type BookingTimeBlockResponse,
  type CreateBookingTimeBlockRequest,
  type SlotResponse,
  type UpdateBookingCalendarDayRequest,
  type UpdateBookingCalendarSettingsRequest,
} from "@/generated/api/adminCatalog";
import { adminHeaders } from "@/shared/api";

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

export function fetchBookingCalendar(
  adminKey: string,
  dateFrom: string,
  dateTo: string,
): Promise<BookingCalendarResponse> {
  return getAdminBookingCalendar(
    { dateFrom, dateTo },
    { headers: adminHeaders(adminKey) },
  );
}

export function saveBookingCalendarSettings(
  adminKey: string,
  body: UpdateBookingCalendarSettingsRequest,
): Promise<BookingCalendarSettingsResponse> {
  return updateAdminBookingCalendarSettings(body, { headers: adminHeaders(adminKey) });
}

export function saveBookingCalendarDay(
  adminKey: string,
  date: string,
  body: UpdateBookingCalendarDayRequest,
): Promise<void> {
  return updateAdminBookingCalendarDay(date, body, { headers: adminHeaders(adminKey) });
}

export function createBookingTimeBlock(
  adminKey: string,
  body: CreateBookingTimeBlockRequest,
): Promise<BookingTimeBlockResponse> {
  return createAdminBookingTimeBlock(body, { headers: adminHeaders(adminKey) });
}

export function deleteBookingTimeBlock(adminKey: string, id: number): Promise<void> {
  return deleteAdminBookingTimeBlock(id, { headers: adminHeaders(adminKey) });
}

export function deactivateSlot(adminKey: string, slotId: number): Promise<SlotResponse> {
  return deactivateAdminSlot(slotId, { headers: adminHeaders(adminKey) });
}

export function activateSlot(adminKey: string, slotId: number): Promise<SlotResponse> {
  return activateAdminSlot(slotId, { headers: adminHeaders(adminKey) });
}
