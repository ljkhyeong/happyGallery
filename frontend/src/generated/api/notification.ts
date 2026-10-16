import { generatedApiClient } from '../../shared/api/generatedClient';
/**
 * @nullable
 */
export type NotificationResponseAggregateType = typeof NotificationResponseAggregateType[keyof typeof NotificationResponseAggregateType] | null;


export const NotificationResponseAggregateType = {
  ORDER: 'ORDER',
  ORDER_CLAIM: 'ORDER_CLAIM',
  BOOKING: 'BOOKING',
  VACANCY_ALERT: 'VACANCY_ALERT',
  PASS_PURCHASE: 'PASS_PURCHASE',
  REFUND: 'REFUND',
  INQUIRY: 'INQUIRY',
  PRODUCT_QNA: 'PRODUCT_QNA',
  RESTOCK_ALERT: 'RESTOCK_ALERT',
  REVIEW: 'REVIEW',
  REVIEW_MODERATION_ACTION: 'REVIEW_MODERATION_ACTION',
} as const;

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

export interface NotificationTarget {
  /**
     * 예약·예약금 환불 알림의 예약 ID
     * @nullable
     */
  bookingId: number | null;
  /**
     * 빈자리 알림 회차의 클래스 ID
     * @nullable
     */
  classId: number | null;
  /**
     * 주문·주문 클레임·주문 환불 알림의 주문 ID
     * @nullable
     */
  orderId: number | null;
  /**
     * 재입고·상품 문의 답변 알림의 상품 ID
     * @nullable
     */
  productId: number | null;
  /**
     * 재입고 알림을 신청한 옵션 조합 ID. 상품 전체로 신청했으면 null
     * @nullable
     */
  productVariantId: number | null;
  /**
     * 빈자리 알림 회차 ID
     * @nullable
     */
  slotId: number | null;
}

export interface NotificationResponse {
  /** @nullable */
  aggregateId: number | null;
  /** @nullable */
  aggregateType: NotificationResponseAggregateType;
  /**
     * 본인 주문의 구매 당시 상품명, 현재 예약 클래스, 빈자리 알림 클래스, 재입고 상품·옵션 또는 문의한 상품. 원본이 없거나 타인 소유이면 null
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
     * 예약 알림은 현재 예약일시, 빈자리 알림은 신청한 회차 시작 시각. 알림 발생 당시 일정이 아니며 그 밖의 알림이거나 원본을 조회할 수 없으면 null
     * @nullable
     */
  scheduledAt: string | null;
  /** 알림을 눌렀을 때 열 본인 기록. 원본이 없거나 타인 소유이면 모든 값이 null */
  target: NotificationTarget;
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

export const listMyNotifications = async (params?: ListMyNotificationsParams, options?: Parameters<typeof generatedApiClient>[1]): Promise<NotificationResponse[]> => {

  return generatedApiClient<NotificationResponse[]>(getListMyNotificationsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getMarkAllMyNotificationsAsReadUrl = () => {




  return `/api/v1/me/notifications/read-all`
}

export const markAllMyNotificationsAsRead = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

  return generatedApiClient<void>(getMarkAllMyNotificationsAsReadUrl(),
  {
    ...options,
    method: 'PATCH'


  }
);}



export const getGetMyUnreadNotificationCountUrl = () => {




  return `/api/v1/me/notifications/unread-count`
}

export const getMyUnreadNotificationCount = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<UnreadCountResponse> => {

  return generatedApiClient<UnreadCountResponse>(getGetMyUnreadNotificationCountUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getMarkMyNotificationAsReadUrl = (id: number,) => {




  return `/api/v1/me/notifications/${id}/read`
}

export const markMyNotificationAsRead = async (id: number, options?: Parameters<typeof generatedApiClient>[1]): Promise<void> => {

  return generatedApiClient<void>(getMarkMyNotificationAsReadUrl(id),
  {
    ...options,
    method: 'PATCH'


  }
);}
