import {
  getPublicEvent,
  listPublicEvents,
  type EventResponse,
} from "@/generated/api/event";
import { queryKeys } from "@/shared/api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { eventRefetchInterval } from "./time";

export type { EventResponse } from "@/generated/api/event";

export function fetchEvents(signal?: AbortSignal): Promise<EventResponse[]> {
  return listPublicEvents({ signal });
}

export function fetchEvent(id: number, signal?: AbortSignal): Promise<EventResponse> {
  return getPublicEvent(id, { signal });
}

/** 홈 추천 이벤트와 이벤트 목록이 같은 캐시·시작/종료 시각 갱신 주기를 쓴다. */
export function publicEventsQuery() {
  return {
    queryKey: queryKeys.events.all,
    queryFn: ({ signal }: { signal: AbortSignal }) => fetchEvents(signal),
    staleTime: PUBLIC_DATA_STALE_TIME,
    refetchInterval: ({ state }: { state: { data: EventResponse[] | undefined } }) =>
      eventRefetchInterval(state.data),
  };
}
