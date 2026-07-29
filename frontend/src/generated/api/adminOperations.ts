import { generatedApiClient } from '../../shared/api/generatedClient';
export interface AdminInquiryResponse {
  content: string;
  createdAt: string;
  id: number;
  /** @nullable */
  repliedAt: string | null;
  /** @nullable */
  replyContent: string | null;
  title: string;
  userId: number;
  userName: string;
}

export interface AdminInquiryPageResponse {
  content: AdminInquiryResponse[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
}

export interface InquiryReplyRequest {
  /** @minLength 1 */
  replyContent: string;
}

export type FailedNotificationResponseEventType = typeof FailedNotificationResponseEventType[keyof typeof FailedNotificationResponseEventType];


export const FailedNotificationResponseEventType = {
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

export type FailedNotificationResponseRecipientType = typeof FailedNotificationResponseRecipientType[keyof typeof FailedNotificationResponseRecipientType];


export const FailedNotificationResponseRecipientType = {
  GUEST: 'GUEST',
  USER: 'USER',
} as const;

export type FailedNotificationResponseStatus = typeof FailedNotificationResponseStatus[keyof typeof FailedNotificationResponseStatus];


export const FailedNotificationResponseStatus = {
  FAILED: 'FAILED',
  PENDING: 'PENDING',
} as const;

export interface FailedNotificationResponse {
  /** @nullable */
  aggregateId: number | null;
  /** @nullable */
  aggregateType: string | null;
  attemptCount: number;
  createdAt: string;
  eventType: FailedNotificationResponseEventType;
  /** @nullable */
  lastError: string | null;
  outboxId: number;
  recipientId: number;
  recipientType: FailedNotificationResponseRecipientType;
  status: FailedNotificationResponseStatus;
}

export type BatchResponseFailureReasons = {[key: string]: number};

export interface BatchResponse {
  failureCount: number;
  failureReasons: BatchResponseFailureReasons;
  successCount: number;
}

/**
 * @nullable
 */
export type AdminPassResponseRefundStatus = typeof AdminPassResponseRefundStatus[keyof typeof AdminPassResponseRefundStatus] | null;


export const AdminPassResponseRefundStatus = {
  REQUESTED: 'REQUESTED',
  PROCESSING: 'PROCESSING',
  RETRYABLE: 'RETRYABLE',
  RECONCILIATION_REQUIRED: 'RECONCILIATION_REQUIRED',
  SUCCEEDED: 'SUCCEEDED',
  FAILED: 'FAILED',
} as const;

export type AdminPassResponseStatus = typeof AdminPassResponseStatus[keyof typeof AdminPassResponseStatus];


export const AdminPassResponseStatus = {
  ACTIVE: 'ACTIVE',
  USED_UP: 'USED_UP',
  EXPIRED: 'EXPIRED',
  REFUND_PENDING: 'REFUND_PENDING',
  REFUND_FAILED: 'REFUND_FAILED',
  REFUNDED: 'REFUNDED',
} as const;

export interface AdminPassResponse {
  customerName: string;
  /** @nullable */
  customerPhone: string | null;
  expectedRefundAmount: number;
  expiresAt: string;
  futureBookingCount: number;
  passId: number;
  passNumber: string;
  /** @nullable */
  refundStatus: AdminPassResponseRefundStatus;
  remainingCredits: number;
  status: AdminPassResponseStatus;
  totalCredits: number;
}

export interface AdminPassPageResponse {
  content: AdminPassResponse[];
  page: number;
  size: number;
  totalCount: number;
  totalPages: number;
}

/**
 * @nullable
 */
export type PassRefundResponseRefundStatus = typeof PassRefundResponseRefundStatus[keyof typeof PassRefundResponseRefundStatus] | null;


export const PassRefundResponseRefundStatus = {
  REQUESTED: 'REQUESTED',
  PROCESSING: 'PROCESSING',
  RETRYABLE: 'RETRYABLE',
  RECONCILIATION_REQUIRED: 'RECONCILIATION_REQUIRED',
  SUCCEEDED: 'SUCCEEDED',
  FAILED: 'FAILED',
} as const;

export interface PassRefundResponse {
  canceledBookings: number;
  refundAmount: number;
  refundCredits: number;
  /** @nullable */
  refundId: number | null;
  /** @nullable */
  refundStatus: PassRefundResponseRefundStatus;
}

export type PaymentReconciliationRequiredResponseContext = typeof PaymentReconciliationRequiredResponseContext[keyof typeof PaymentReconciliationRequiredResponseContext];


export const PaymentReconciliationRequiredResponseContext = {
  ORDER: 'ORDER',
  BOOKING: 'BOOKING',
  PASS: 'PASS',
} as const;

export type PaymentReconciliationRequiredResponseStatus = typeof PaymentReconciliationRequiredResponseStatus[keyof typeof PaymentReconciliationRequiredResponseStatus];


export const PaymentReconciliationRequiredResponseStatus = {
  RECONCILIATION_REQUIRED: 'RECONCILIATION_REQUIRED',
} as const;

export interface PaymentReconciliationRequiredResponse {
  amount: number;
  attemptId: number;
  context: PaymentReconciliationRequiredResponseContext;
  createdAt: string;
  /** @nullable */
  reason: string | null;
  status: PaymentReconciliationRequiredResponseStatus;
}

export type PaymentReconciliationResultResponseStatus = typeof PaymentReconciliationResultResponseStatus[keyof typeof PaymentReconciliationResultResponseStatus];


export const PaymentReconciliationResultResponseStatus = {
  RECONCILIATION_REQUIRED: 'RECONCILIATION_REQUIRED',
  CONFIRMED: 'CONFIRMED',
  FAILED: 'FAILED',
} as const;

export interface PaymentReconciliationResultResponse {
  attemptId: number;
  /** @nullable */
  domainId: number | null;
  message: string;
  status: PaymentReconciliationResultResponseStatus;
}

export type FailedRefundResponseStatus = typeof FailedRefundResponseStatus[keyof typeof FailedRefundResponseStatus];


export const FailedRefundResponseStatus = {
  FAILED: 'FAILED',
  RETRYABLE: 'RETRYABLE',
  RECONCILIATION_REQUIRED: 'RECONCILIATION_REQUIRED',
} as const;

export interface FailedRefundResponse {
  amount: number;
  attemptCount: number;
  /** @nullable */
  bookingId: number | null;
  createdAt: string;
  failReason: string;
  /** @nullable */
  orderClaimId: number | null;
  /** @nullable */
  orderId: number | null;
  /** @nullable */
  passPurchaseId: number | null;
  /** @nullable */
  paymentAttemptId: number | null;
  refundId: number;
  status: FailedRefundResponseStatus;
}

export interface FailedRefundPageResponse {
  content: FailedRefundResponse[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
}

export type RefundStatusResponseStatus = typeof RefundStatusResponseStatus[keyof typeof RefundStatusResponseStatus];


export const RefundStatusResponseStatus = {
  REQUESTED: 'REQUESTED',
  PROCESSING: 'PROCESSING',
  RETRYABLE: 'RETRYABLE',
  RECONCILIATION_REQUIRED: 'RECONCILIATION_REQUIRED',
  SUCCEEDED: 'SUCCEEDED',
  FAILED: 'FAILED',
} as const;

export interface RefundStatusResponse {
  amount: number;
  attemptCount: number;
  /** @nullable */
  failReason: string | null;
  refundId: number;
  status: RefundStatusResponseStatus;
}

export type ListAdminInquiriesParams = {
cursor?: string;
size?: number;
};

export type SearchAdminPassesParams = {
keyword?: string;
page?: number;
size?: number;
};

export type ListFailedParams = {
cursor?: string;
size?: number;
};

export const getListAdminInquiriesUrl = (params?: ListAdminInquiriesParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/inquiries?${stringifiedParams}` : `/api/v1/admin/inquiries`
}

export const listAdminInquiries = async (params?: ListAdminInquiriesParams, options?: RequestInit): Promise<AdminInquiryPageResponse> => {

  return generatedApiClient<AdminInquiryPageResponse>(getListAdminInquiriesUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetAdminInquiryUrl = (id: number,) => {




  return `/api/v1/admin/inquiries/${id}`
}

export const getAdminInquiry = async (id: number, options?: RequestInit): Promise<AdminInquiryResponse> => {

  return generatedApiClient<AdminInquiryResponse>(getGetAdminInquiryUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getReplyToAdminInquiryUrl = (id: number,) => {




  return `/api/v1/admin/inquiries/${id}/reply`
}

export const replyToAdminInquiry = async (id: number,
    inquiryReplyRequest: InquiryReplyRequest, options?: RequestInit): Promise<AdminInquiryResponse> => {

  return generatedApiClient<AdminInquiryResponse>(getReplyToAdminInquiryUrl(id),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(inquiryReplyRequest)
  }
);}



export const getListFailedNotificationsUrl = () => {




  return `/api/v1/admin/notifications/failed`
}

export const listFailedNotifications = async ( options?: RequestInit): Promise<FailedNotificationResponse[]> => {

  return generatedApiClient<FailedNotificationResponse[]>(getListFailedNotificationsUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRetryNotificationUrl = (outboxId: number,) => {




  return `/api/v1/admin/notifications/${outboxId}/retry`
}

export const retryNotification = async (outboxId: number, options?: RequestInit): Promise<FailedNotificationResponse> => {

  return generatedApiClient<FailedNotificationResponse>(getRetryNotificationUrl(outboxId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getTriggerExpiryUrl = () => {




  return `/api/v1/admin/passes/expire`
}

export const triggerExpiry = async ( options?: RequestInit): Promise<BatchResponse> => {

  return generatedApiClient<BatchResponse>(getTriggerExpiryUrl(),
  {
    ...options,
    method: 'POST'


  }
);}



export const getSearchAdminPassesUrl = (params?: SearchAdminPassesParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/passes/search?${stringifiedParams}` : `/api/v1/admin/passes/search`
}

export const searchAdminPasses = async (params?: SearchAdminPassesParams, options?: RequestInit): Promise<AdminPassPageResponse> => {

  return generatedApiClient<AdminPassPageResponse>(getSearchAdminPassesUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetAdminPassUrl = (passId: number,) => {




  return `/api/v1/admin/passes/${passId}`
}

export const getAdminPass = async (passId: number, options?: RequestInit): Promise<AdminPassResponse> => {

  return generatedApiClient<AdminPassResponse>(getGetAdminPassUrl(passId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRefundPassUrl = (passId: number,) => {




  return `/api/v1/admin/passes/${passId}/refund`
}

export const refundPass = async (passId: number, options?: RequestInit): Promise<PassRefundResponse> => {

  return generatedApiClient<PassRefundResponse>(getRefundPassUrl(passId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getListRequiredUrl = () => {




  return `/api/v1/admin/payment-attempts/reconciliation-required`
}

export const listRequired = async ( options?: RequestInit): Promise<PaymentReconciliationRequiredResponse[]> => {

  return generatedApiClient<PaymentReconciliationRequiredResponse[]>(getListRequiredUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getReconcileUrl = (attemptId: number,) => {




  return `/api/v1/admin/payment-attempts/${attemptId}/reconcile`
}

export const reconcile = async (attemptId: number, options?: RequestInit): Promise<PaymentReconciliationResultResponse> => {

  return generatedApiClient<PaymentReconciliationResultResponse>(getReconcileUrl(attemptId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getListFailedUrl = (params?: ListFailedParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/refunds/failed?${stringifiedParams}` : `/api/v1/admin/refunds/failed`
}

export const listFailed = async (params?: ListFailedParams, options?: RequestInit): Promise<FailedRefundPageResponse> => {

  return generatedApiClient<FailedRefundPageResponse>(getListFailedUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetRefundUrl = (refundId: number,) => {




  return `/api/v1/admin/refunds/${refundId}`
}

export const getRefund = async (refundId: number, options?: RequestInit): Promise<RefundStatusResponse> => {

  return generatedApiClient<RefundStatusResponse>(getGetRefundUrl(refundId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRetryUrl = (refundId: number,) => {




  return `/api/v1/admin/refunds/${refundId}/retry`
}

export const retry = async (refundId: number, options?: RequestInit): Promise<RefundStatusResponse> => {

  return generatedApiClient<RefundStatusResponse>(getRetryUrl(refundId),
  {
    ...options,
    method: 'POST'


  }
);}
