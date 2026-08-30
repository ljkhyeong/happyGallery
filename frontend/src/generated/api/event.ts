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

export const getListPublicEventsUrl = () => {




  return `/api/v1/events`
}

export const listPublicEvents = async ( options?: RequestInit): Promise<EventResponse[]> => {

  return generatedApiClient<EventResponse[]>(getListPublicEventsUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetPublicEventUrl = (id: number,) => {




  return `/api/v1/events/${id}`
}

export const getPublicEvent = async (id: number, options?: RequestInit): Promise<EventResponse> => {

  return generatedApiClient<EventResponse>(getGetPublicEventUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}
