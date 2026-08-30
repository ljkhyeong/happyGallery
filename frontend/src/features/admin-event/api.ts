import {
  createAdminEvent,
  deleteAdminEvent,
  getAdminEvent,
  listAdminEvents,
  updateAdminEvent,
  type CreateEventRequest,
  type EventResponse,
  type UpdateEventRequest,
} from "@/generated/api/adminEvent";
import { adminHeaders } from "@/shared/api";

export type {
  CreateEventRequest,
  EventResponse,
  UpdateEventRequest,
} from "@/generated/api/adminEvent";

export function fetchAdminEvents(token: string): Promise<EventResponse[]> {
  return listAdminEvents({ headers: adminHeaders(token) });
}

export function fetchAdminEvent(id: number, token: string): Promise<EventResponse> {
  return getAdminEvent(id, { headers: adminHeaders(token) });
}

export function createEvent(
  request: CreateEventRequest,
  token: string,
): Promise<EventResponse> {
  return createAdminEvent(request, { headers: adminHeaders(token) });
}

export function updateEvent(
  id: number,
  request: UpdateEventRequest,
  token: string,
): Promise<EventResponse> {
  return updateAdminEvent(id, request, { headers: adminHeaders(token) });
}

export function deleteEvent(
  id: number,
  expectedVersion: number,
  token: string,
): Promise<void> {
  return deleteAdminEvent(
    id,
    { expectedVersion },
    { headers: adminHeaders(token) },
  );
}
