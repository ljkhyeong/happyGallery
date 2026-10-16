import { generatedApiClient } from '../../shared/api/generatedClient';
export interface EventResponse {
  content: string;
  /** @nullable */
  couponDefinitionId: number | null;
  endAt: string;
  featured: boolean;
  id: number;
  /** @nullable */
  imageUrl: string | null;
  published: boolean;
  relatedProductIds: number[];
  startAt: string;
  summary: string;
  title: string;
  version: number;
}

export interface CreateEventRequest {
  /**
     * @minLength 0
     * @maxLength 16000
     */
  content: string;
  /** @nullable */
  couponDefinitionId?: number | null;
  endAt: string;
  featured: boolean;
  /**
     * @minLength 0
     * @maxLength 500
     * @nullable
     */
  imageUrl?: string | null;
  published: boolean;
  /** @nullable */
  relatedProductIds?: number[] | null;
  startAt: string;
  /**
     * @minLength 0
     * @maxLength 500
     */
  summary: string;
  /**
     * @minLength 0
     * @maxLength 200
     */
  title: string;
}

export interface UpdateEventRequest {
  /**
     * @minLength 0
     * @maxLength 16000
     */
  content: string;
  /** @nullable */
  couponDefinitionId?: number | null;
  endAt: string;
  expectedVersion: number;
  featured: boolean;
  /**
     * @minLength 0
     * @maxLength 500
     * @nullable
     */
  imageUrl?: string | null;
  published: boolean;
  /** @nullable */
  relatedProductIds?: number[] | null;
  startAt: string;
  /**
     * @minLength 0
     * @maxLength 500
     */
  summary: string;
  /**
     * @minLength 0
     * @maxLength 200
     */
  title: string;
}

export type DeleteAdminEventParams = {
/**
 * @minimum 0
 */
expectedVersion: number;
};

export const getListAdminEventsUrl = () => {




  return `/api/v1/admin/events`
}

export const listAdminEvents = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<EventResponse[]> => {

  return generatedApiClient<EventResponse[]>(getListAdminEventsUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCreateAdminEventUrl = () => {




  return `/api/v1/admin/events`
}

export const createAdminEvent = async (createEventRequest: CreateEventRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<EventResponse> => {

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
return generatedApiClient<EventResponse>(getCreateAdminEventUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(createEventRequest)
  }
);}



export const getDeleteAdminEventUrl = (id: number,
    params: DeleteAdminEventParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/events/${id}?${stringifiedParams}` : `/api/v1/admin/events/${id}`
}

export const deleteAdminEvent = async (id: number,
    params: DeleteAdminEventParams, options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

  return generatedApiClient<void>(getDeleteAdminEventUrl(id,params),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getGetAdminEventUrl = (id: number,) => {




  return `/api/v1/admin/events/${id}`
}

export const getAdminEvent = async (id: number, options?: Parameters<typeof generatedApiClient>[1]): Promise<EventResponse> => {

  return generatedApiClient<EventResponse>(getGetAdminEventUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getUpdateAdminEventUrl = (id: number,) => {




  return `/api/v1/admin/events/${id}`
}

export const updateAdminEvent = async (id: number,
    updateEventRequest: UpdateEventRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<EventResponse> => {

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
return generatedApiClient<EventResponse>(getUpdateAdminEventUrl(id),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(updateEventRequest)
  }
);}
