import {
  getPublicEvent,
  listPublicEvents,
  type EventResponse,
} from "@/generated/api/event";

export type { EventResponse } from "@/generated/api/event";

export function fetchEvents(): Promise<EventResponse[]> {
  return listPublicEvents();
}

export function fetchEvent(id: number): Promise<EventResponse> {
  return getPublicEvent(id);
}
