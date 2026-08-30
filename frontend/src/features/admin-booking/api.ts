import {
  cancelAdminBooking,
  complete,
  completeBookingCancellationTask as completeBookingCancellationTaskRequest,
  createAdminBooking as createAdminBookingRequest,
  listBookings,
  listPendingBookingCancellationTasks,
  markBalancePaid as markBalancePaidRequest,
  markNoShow as markNoShowRequest,
  updateArrears as updateArrearsRequest,
} from "@/generated/api/adminBooking";
import type {
  AdminBookingCancelRequest,
  CreateAdminBookingRequest,
  ListBookingsStatus,
} from "@/generated/api/adminBooking";
import { adminHeaders } from "@/shared/api";

export type {
  BookingCancellationTaskCompletionResponse as BookingCancellationTaskCompletion,
  BookingCancellationTaskResponse as BookingCancellationTask,
  BookingCancellationTaskResponseType as BookingCancellationTaskType,
} from "@/generated/api/adminBooking";

export function fetchBookings(
  adminKey: string,
  date: string,
  status?: ListBookingsStatus,
) {
  return listBookings({ date, status }, {
    headers: adminHeaders(adminKey),
  });
}

export function createBookingByAdmin(
  adminKey: string,
  body: CreateAdminBookingRequest,
) {
  return createAdminBookingRequest(body, {
    headers: adminHeaders(adminKey),
  });
}

export function cancelBookingByAdmin(
  adminKey: string,
  bookingId: number,
  body: AdminBookingCancelRequest,
) {
  return cancelAdminBooking(bookingId, body, {
    headers: adminHeaders(adminKey),
  });
}

export function markNoShow(
  adminKey: string,
  bookingId: number,
) {
  return markNoShowRequest(bookingId, {
    headers: adminHeaders(adminKey),
  });
}

export function markBalancePaid(
  adminKey: string,
  bookingId: number,
) {
  return markBalancePaidRequest(bookingId, {
    headers: adminHeaders(adminKey),
  });
}

export function updateArrears(
  adminKey: string,
  bookingId: number,
  arrears: boolean,
) {
  return updateArrearsRequest(bookingId, { arrears }, {
    headers: adminHeaders(adminKey),
  });
}

export function completeBooking(
  adminKey: string,
  bookingId: number,
) {
  return complete(bookingId, {
    headers: adminHeaders(adminKey),
  });
}

export function fetchBookingCancellationTasks(
  adminKey: string,
) {
  return listPendingBookingCancellationTasks({
    headers: adminHeaders(adminKey),
  });
}

export function completeBookingCancellationTask(
  adminKey: string,
  taskId: number,
) {
  return completeBookingCancellationTaskRequest(taskId, {
    headers: adminHeaders(adminKey),
  });
}
