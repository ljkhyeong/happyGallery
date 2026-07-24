import { generatedApiClient } from '../../shared/api/generatedClient';
export type NotificationResponseEventType = typeof NotificationResponseEventType[keyof typeof NotificationResponseEventType];


export const NotificationResponseEventType = {
  BOOKING_CONFIRMED: 'BOOKING_CONFIRMED',
  BOOKING_RESCHEDULED: 'BOOKING_RESCHEDULED',
  BOOKING_CANCELED: 'BOOKING_CANCELED',
  DEPOSIT_REFUNDED: 'DEPOSIT_REFUNDED',
  ORDER_PAID: 'ORDER_PAID',
  ORDER_APPROVED: 'ORDER_APPROVED',
  ORDER_PICKUP_READY: 'ORDER_PICKUP_READY',
  ORDER_SHIPPED: 'ORDER_SHIPPED',
  ORDER_DELAY_REQUESTED: 'ORDER_DELAY_REQUESTED',
  ORDER_REFUNDED: 'ORDER_REFUNDED',
  ORDER_CLAIM_RESOLVED: 'ORDER_CLAIM_RESOLVED',
  ORDER_EXCHANGE_COMPLETED: 'ORDER_EXCHANGE_COMPLETED',
  PASS_PURCHASED: 'PASS_PURCHASED',
  PASS_REFUNDED: 'PASS_REFUNDED',
  INQUIRY_ANSWERED: 'INQUIRY_ANSWERED',
  PRODUCT_QNA_ANSWERED: 'PRODUCT_QNA_ANSWERED',
  REMINDER_D1: 'REMINDER_D1',
  REMINDER_SAME_DAY: 'REMINDER_SAME_DAY',
  PASS_EXPIRY_SOON: 'PASS_EXPIRY_SOON',
  PICKUP_DEADLINE_REMINDER: 'PICKUP_DEADLINE_REMINDER',
} as const;

export interface NotificationResponse {
  /** @nullable */
  aggregateId: number | null;
  /** @nullable */
  aggregateType: string | null;
  deliveredAt: string;
  eventType: NotificationResponseEventType;
  id: number;
  read: boolean;
  /** @nullable */
  readAt: string | null;
}

export interface UnreadCountResponse {
  count: number;
}

export type ListMyNotificationsParams = {
page?: number;
size?: number;
};

export const getListMyNotificationsUrl = (params?: ListMyNotificationsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/me/notifications?${stringifiedParams}` : `/api/v1/me/notifications`
}

export const listMyNotifications = async (params?: ListMyNotificationsParams, options?: RequestInit): Promise<NotificationResponse[]> => {

  return generatedApiClient<NotificationResponse[]>(getListMyNotificationsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getMarkAllMyNotificationsAsReadUrl = () => {




  return `/api/v1/me/notifications/read-all`
}

export const markAllMyNotificationsAsRead = async ( options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getMarkAllMyNotificationsAsReadUrl(),
  {
    ...options,
    method: 'PATCH'


  }
);}



export const getGetMyUnreadNotificationCountUrl = () => {




  return `/api/v1/me/notifications/unread-count`
}

export const getMyUnreadNotificationCount = async ( options?: RequestInit): Promise<UnreadCountResponse> => {

  return generatedApiClient<UnreadCountResponse>(getGetMyUnreadNotificationCountUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getMarkMyNotificationAsReadUrl = (id: number,) => {




  return `/api/v1/me/notifications/${id}/read`
}

export const markMyNotificationAsRead = async (id: number, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getMarkMyNotificationAsReadUrl(id),
  {
    ...options,
    method: 'PATCH'


  }
);}
