import { api } from "@/shared/api";

const CLIENT_MONITORING_ENDPOINT = "/monitoring/client-events";

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

  const body = {
    event: input.event,
    path: input.path ?? window.location.pathname,
    source: input.source,
    target: input.target,
  };

  void api<void>(CLIENT_MONITORING_ENDPOINT, {
    method: "POST",
    body,
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
