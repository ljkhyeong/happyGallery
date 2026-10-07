import {
  captureClientEvent,
  type CaptureClientEventRequestEvent,
} from "@/generated/api/monitoring";

export type ClientMonitoringEvent = CaptureClientEventRequestEvent;

export function trackClientEvent(event: ClientMonitoringEvent) {
  if (typeof window === "undefined") return;

  void captureClientEvent({ event }, { keepalive: true }).catch(() => {
    // monitoring is best-effort
  });
}
