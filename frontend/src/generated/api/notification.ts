import { generatedApiClient } from '../../shared/api/generatedClient';
export type NotificationResponseEventType = typeof NotificationResponseEventType[keyof typeof NotificationResponseEventType];


export const NotificationResponseEventType = {
  BOOKING_CONFIRMED: 'BOOKING_CONFIRMED',
  BOOKING_RESCHEDULED: 'BOOKING_RESCHEDULED',
  BOOKING_CANCELED: 'BOOKING_CANCELED',
  BOOKING_VACANCY_AVAILABLE: 'BOOKING_VACANCY_AVAILABLE',
  PRODUCT_RESTOCK_AVAILABLE: 'PRODUCT_RESTOCK_AVAILABLE',
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
  REVIEW_REQUEST: 'REVIEW_REQUEST',
  REVIEW_HIDDEN: 'REVIEW_HIDDEN',
  REVIEW_REPUBLISHED: 'REVIEW_REPUBLISHED',
  REVIEW_OWNER_REPLIED: 'REVIEW_OWNER_REPLIED',
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
  /**
     * 본인 주문의 구매 당시 상품명, 현재 예약 클래스 또는 재입고 상품·옵션. 원본이 없거나 타인 소유이면 null
     * @nullable
     */
  contextTitle: string | null;
  deliveredAt: string;
  eventType: NotificationResponseEventType;
  id: number;
  read: boolean;
  /** @nullable */
  readAt: string | null;
  /**
     * 예약 알림 원본의 현재 예약일시. 알림 발생 당시 일정이 아니며 예약이 아니거나 원본을 조회할 수 없으면 null
     * @nullable
     */
  scheduledAt: string | null;
}

export interface UnreadCountResponse {
  count: number;
}

export type ListMyNotificationsParams = {
page?: number;
size?: number;
unreadOnly?: boolean;
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
