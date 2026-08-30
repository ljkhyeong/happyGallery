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
  /**
     * @minLength 1
     * @maxLength 16000
     */
  replyContent: string;
}

export type FailedNotificationResponseEventType = typeof FailedNotificationResponseEventType[keyof typeof FailedNotificationResponseEventType];


export const FailedNotificationResponseEventType = {
  BOOKING_CONFIRMED: 'BOOKING_CONFIRMED',
  BOOKING_RESCHEDULED: 'BOOKING_RESCHEDULED',
  BOOKING_CANCELED: 'BOOKING_CANCELED',
  BOOKING_VACANCY_AVAILABLE: 'BOOKING_VACANCY_AVAILABLE',
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

export type PaymentSettlementIssueResponseStatus = typeof PaymentSettlementIssueResponseStatus[keyof typeof PaymentSettlementIssueResponseStatus];


export const PaymentSettlementIssueResponseStatus = {
  MATCHED: 'MATCHED',
  LOCAL_PAYMENT_NOT_FOUND: 'LOCAL_PAYMENT_NOT_FOUND',
  LOCAL_REFUND_NOT_FOUND: 'LOCAL_REFUND_NOT_FOUND',
  IDENTIFIER_MISMATCH: 'IDENTIFIER_MISMATCH',
  AMOUNT_MISMATCH: 'AMOUNT_MISMATCH',
} as const;

export interface PaymentSettlementIssueResponse {
  amount: number;
  cancelTransaction: boolean;
  fetchedAt: string;
  id: number;
  /** @nullable */
  orderId: string | null;
  payOutAmount: number;
  paymentKey: string;
  /** @nullable */
  reason: string | null;
  soldDate: string;
  status: PaymentSettlementIssueResponseStatus;
  transactionKey: string;
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
  pgRefundAmount: number;
  refundId: number;
  restoreCoupon: boolean;
  rewardRestoreAmount: number;
  rewardRevokeAmount: number;
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
  pgRefundAmount: number;
  refundId: number;
  restoreCoupon: boolean;
  rewardRestoreAmount: number;
  rewardRevokeAmount: number;
  status: RefundStatusResponseStatus;
}

export interface CommissionDetailResponse {
  commissionAmount: number;
  commissionBasisAmount: number;
  commissionType: string;
  /** @nullable */
  maximumSellingInterlockCommissionAmount: number | null;
  merchantId: string;
  merchantName: string;
  orderNo: string;
  /** @nullable */
  payMeansType: string | null;
  /** @nullable */
  productId: string | null;
  /** @nullable */
  productName: string | null;
  productOrderId: string;
  productOrderType: string;
  /** @nullable */
  settleBasisDate: string | null;
  /** @nullable */
  settleCompleteDate: string | null;
  /** @nullable */
  settleExpectDate: string | null;
  settleType: string;
  /** @nullable */
  taxReturnDate: string | null;
}

export interface DailySettlementResponse {
  benefitSettleAmount: number;
  commissionSettleAmount: number;
  deductionRestoreSettleAmount: number;
  differenceSettleAmount: number;
  /** @nullable */
  merchantId: string | null;
  /** @nullable */
  merchantName: string | null;
  minusChargeAmount: number;
  normalSettleAmount: number;
  payHoldbackAmount: number;
  paySettleAmount: number;
  preferentialCommissionAmount: number;
  quickSettleAmount: number;
  returnCareSettleAmount: number;
  settleAmount: number;
  settleBasisEndDate: string;
  settleBasisStartDate: string;
  /** @nullable */
  settleCompleteDate: string | null;
  settleExpectDate: string;
  settleMethodType: string;
  settlementLimitAmount: number;
}

export interface DailyVatResponse {
  cashExclusionIssuanceAmount: number;
  cashInComeDeductionAmount: number;
  cashOutGoingEvidenceAmount: number;
  creditCardAmount: number;
  /** @nullable */
  merchantId: string | null;
  /** @nullable */
  merchantName: string | null;
  otherAmount: number;
  settleBasisDate: string;
  taxExemptionSalesAmount: number;
  taxationSalesAmount: number;
  totalSalesAmount: number;
}

export interface SmartStoreAccountingReportResponse {
  commissionDetails: CommissionDetailResponse[];
  dailySettlements: DailySettlementResponse[];
  dailyVat: DailyVatResponse[];
  from: string;
  to: string;
  vatAvailableThrough: string;
}

export type SmartStoreSettlementIssueResponseStatus = typeof SmartStoreSettlementIssueResponseStatus[keyof typeof SmartStoreSettlementIssueResponseStatus];


export const SmartStoreSettlementIssueResponseStatus = {
  MATCHED: 'MATCHED',
  ORDER_NOT_FOUND: 'ORDER_NOT_FOUND',
  EXPECTED_AMOUNT_MISSING: 'EXPECTED_AMOUNT_MISSING',
  AMOUNT_MISMATCH: 'AMOUNT_MISMATCH',
  NOT_APPLICABLE: 'NOT_APPLICABLE',
} as const;

export interface SmartStoreSettlementIssueResponse {
  benefitSettleAmount: number;
  entryKey: string;
  fetchedAt: string;
  /** @nullable */
  orderId: string | null;
  /** @nullable */
  payDate: string | null;
  paySettleAmount: number;
  /** @nullable */
  productName: string | null;
  /** @nullable */
  productOrderId: string | null;
  productOrderType: string;
  /** @nullable */
  reason: string | null;
  /** @nullable */
  sellingInterlockCommissionAmount: number | null;
  /** @nullable */
  settleBasisDate: string | null;
  /** @nullable */
  settleCompleteDate: string | null;
  settleExpectAmount: number;
  /** @nullable */
  settleExpectDate: string | null;
  /** @nullable */
  settleType: string | null;
  status: SmartStoreSettlementIssueResponseStatus;
  /** @nullable */
  totalPayCommissionAmount: number | null;
}

export interface SynchronizeSmartStoreSettlementRequest {
  from: string;
  to: string;
}

export type SmartStoreSettlementSyncResponseIssueReasons = {[key: string]: number};

export interface SmartStoreSettlementSyncResponse {
  issueCount: number;
  issueReasons: SmartStoreSettlementSyncResponseIssueReasons;
  successCount: number;
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

export type GetSmartStoreAccountingReportParams = {
from: string;
to: string;
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



export const getListPaymentSettlementIssuesUrl = () => {




  return `/api/v1/admin/payment-settlements/issues`
}

/**
 * @summary PG 정산 대사 불일치 목록 조회
 */
export const listPaymentSettlementIssues = async ( options?: RequestInit): Promise<PaymentSettlementIssueResponse[]> => {

  return generatedApiClient<PaymentSettlementIssueResponse[]>(getListPaymentSettlementIssuesUrl(),
  {
    ...options,
    method: 'GET'


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



export const getGetSmartStoreAccountingReportUrl = (params: GetSmartStoreAccountingReportParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/smartstore-settlements/accounting?${stringifiedParams}` : `/api/v1/admin/smartstore-settlements/accounting`
}

/**
 * @summary 스마트스토어 일별 정산·수수료·부가세 자료 조회
 */
export const getSmartStoreAccountingReport = async (params: GetSmartStoreAccountingReportParams, options?: RequestInit): Promise<SmartStoreAccountingReportResponse> => {

  return generatedApiClient<SmartStoreAccountingReportResponse>(getGetSmartStoreAccountingReportUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListSmartStoreSettlementIssuesUrl = () => {




  return `/api/v1/admin/smartstore-settlements/issues`
}

/**
 * @summary 스마트스토어 정산 대사 불일치 목록 조회
 */
export const listSmartStoreSettlementIssues = async ( options?: RequestInit): Promise<SmartStoreSettlementIssueResponse[]> => {

  return generatedApiClient<SmartStoreSettlementIssueResponse[]>(getListSmartStoreSettlementIssuesUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getSynchronizeSmartStoreSettlementsUrl = () => {




  return `/api/v1/admin/smartstore-settlements/synchronize`
}

/**
 * @summary 스마트스토어 정산 기간 재동기화
 */
export const synchronizeSmartStoreSettlements = async (synchronizeSmartStoreSettlementRequest: SynchronizeSmartStoreSettlementRequest, options?: RequestInit): Promise<SmartStoreSettlementSyncResponse> => {

  return generatedApiClient<SmartStoreSettlementSyncResponse>(getSynchronizeSmartStoreSettlementsUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(synchronizeSmartStoreSettlementRequest)
  }
);}
