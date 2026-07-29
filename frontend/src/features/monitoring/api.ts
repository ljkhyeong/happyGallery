import {
  captureClientEvent,
  type CaptureClientEventRequestEvent,
} from "@/generated/api/monitoring";

export type ClientMonitoringEvent = CaptureClientEventRequestEvent;

interface TrackClientEventInput {
  event: ClientMonitoringEvent;
  path?: string;
  source?: string;
  target?: string;
}

export function trackClientEvent(input: TrackClientEventInput) {
  if (typeof window === "undefined") return;

  const body = {
    event: input.event,
    path: input.path ?? window.location.pathname,
    source: input.source,
    target: input.target,
  };

  void captureClientEvent(body, { keepalive: true }).catch(() => {
    // monitoring is best-effort
  });
}

export function trackGuestMemberCta(source: string, target: "login" | "signup") {
  trackClientEvent({
    event: "GUEST_MEMBER_CTA_CLICKED",
    source,
    target,
  });
}
