import { generatedApiClient } from '../../shared/api/generatedClient';
export type CaptureClientEventRequestEvent = typeof CaptureClientEventRequestEvent[keyof typeof CaptureClientEventRequestEvent];


export const CaptureClientEventRequestEvent = {
  GUEST_LOOKUP_HUB_VIEWED: 'GUEST_LOOKUP_HUB_VIEWED',
  GUEST_ORDER_DIRECT_ENTRY_CONTINUED: 'GUEST_ORDER_DIRECT_ENTRY_CONTINUED',
  GUEST_MEMBER_CTA_CLICKED: 'GUEST_MEMBER_CTA_CLICKED',
  GUEST_CLAIM_MODAL_OPENED: 'GUEST_CLAIM_MODAL_OPENED',
} as const;

export interface CaptureClientEventRequest {
  event: CaptureClientEventRequestEvent;
}

export const getCaptureClientEventUrl = () => {




  return `/api/v1/monitoring/client-events`
}

export const captureClientEvent = async (captureClientEventRequest: CaptureClientEventRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getCaptureClientEventUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(captureClientEventRequest)
  }
);}
