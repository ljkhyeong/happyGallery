import { generatedApiClient } from '../../shared/api/generatedClient';
export type CaptureClientEventRequestEvent = typeof CaptureClientEventRequestEvent[keyof typeof CaptureClientEventRequestEvent];


export const CaptureClientEventRequestEvent = {
  GUEST_LOOKUP_HUB_VIEWED: 'GUEST_LOOKUP_HUB_VIEWED',
  GUEST_ORDER_DIRECT_ENTRY_CONTINUED: 'GUEST_ORDER_DIRECT_ENTRY_CONTINUED',
  GUEST_MEMBER_CTA_CLICKED: 'GUEST_MEMBER_CTA_CLICKED',
  GUEST_CLAIM_MODAL_OPENED: 'GUEST_CLAIM_MODAL_OPENED',
  GUEST_CLAIM_COMPLETED: 'GUEST_CLAIM_COMPLETED',
} as const;

export interface CaptureClientEventRequest {
  event: CaptureClientEventRequestEvent;
  /**
     * @minLength 0
     * @maxLength 120
     */
  path: string;
  /**
     * @minLength 0
     * @maxLength 80
     */
  source?: string;
  /**
     * @minLength 0
     * @maxLength 80
     */
  target?: string;
}

export const getCaptureClientEventUrl = () => {




  return `/api/v1/monitoring/client-events`
}

export const captureClientEvent = async (captureClientEventRequest: CaptureClientEventRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

    const getHeaders = (h?: NonNullable<RequestInit['headers']>): Record<string, string | readonly string[]> => {
    if (!h) return {};
    if (h instanceof Headers) return Object.fromEntries(h.entries());
    if (Symbol.iterator in h) {
      return Object.fromEntries(
        Array.from(h as Iterable<Iterable<string>>, (entry) => Array.from(entry) as [string, string]),
      );
    }
    const headers: Record<string, string | readonly string[]> = {};
    for (const [name, value] of Object.entries<string | readonly string[] | undefined>(h)) {
      if (value !== undefined) headers[name] = value;
    }
    return headers;
  };
return generatedApiClient<void>(getCaptureClientEventUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(captureClientEventRequest)
  }
);}
