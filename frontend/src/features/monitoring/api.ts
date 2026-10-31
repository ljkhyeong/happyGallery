import {
  captureClientEvent,
  type CaptureClientEventRequestEvent,
} from "@/generated/api/monitoring";

export function trackClientEvent(event: CaptureClientEventRequestEvent) {
  if (typeof window === "undefined") return;

  void captureClientEvent({ event }, { keepalive: true }).catch(() => {
    // monitoring is best-effort
  });
}
