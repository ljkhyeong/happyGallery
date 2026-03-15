const CLIENT_MONITORING_ENDPOINT = "/api/v1/monitoring/client-events";

export type ClientMonitoringEvent =
  | "GUEST_LOOKUP_HUB_VIEWED"
  | "GUEST_ORDER_DIRECT_ENTRY_CONTINUED"
  | "GUEST_MEMBER_CTA_CLICKED"
  | "GUEST_CLAIM_MODAL_OPENED";

interface TrackClientEventInput {
  event: ClientMonitoringEvent;
  path?: string;
  source?: string;
  target?: string;
}

export function trackClientEvent(input: TrackClientEventInput) {
  if (typeof window === "undefined") return;

  const body = JSON.stringify({
    event: input.event,
    path: input.path ?? window.location.pathname,
    source: input.source,
    target: input.target,
  });

  try {
    if (typeof navigator !== "undefined" && typeof navigator.sendBeacon === "function") {
      const sent = navigator.sendBeacon(
        CLIENT_MONITORING_ENDPOINT,
        new Blob([body], { type: "application/json" }),
      );
      if (sent) return;
    }
  } catch {
    // sendBeacon fallback below
  }

  void fetch(CLIENT_MONITORING_ENDPOINT, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body,
    credentials: "include",
    keepalive: true,
  }).catch(() => {
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
