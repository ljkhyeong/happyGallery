import {
  getPublicEvent,
  listPublicEvents,
  type EventResponse,
} from "@/generated/api/event";

export type { EventResponse } from "@/generated/api/event";

export function fetchEvents(signal?: AbortSignal): Promise<EventResponse[]> {
  return listPublicEvents({ signal });
}

export function fetchEvent(id: number, signal?: AbortSignal): Promise<EventResponse> {
  return getPublicEvent(id, { signal });
}
