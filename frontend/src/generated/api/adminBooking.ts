import { generatedApiClient } from '../../shared/api/generatedClient';
export type AdminBookingResponseBalanceStatus = typeof AdminBookingResponseBalanceStatus[keyof typeof AdminBookingResponseBalanceStatus];


export const AdminBookingResponseBalanceStatus = {
  UNPAID: 'UNPAID',
  PAID: 'PAID',
} as const;

export type AdminBookingResponseSource = typeof AdminBookingResponseSource[keyof typeof AdminBookingResponseSource];


export const AdminBookingResponseSource = {
  WEB: 'WEB',
  PHONE: 'PHONE',
  NAVER_TALK: 'NAVER_TALK',
  KAKAO: 'KAKAO',
  VISIT: 'VISIT',
} as const;

export type AdminBookingResponseStatus = typeof AdminBookingResponseStatus[keyof typeof AdminBookingResponseStatus];


export const AdminBookingResponseStatus = {
  BOOKED: 'BOOKED',
  CANCELED: 'CANCELED',
  NO_SHOW: 'NO_SHOW',
  COMPLETED: 'COMPLETED',
} as const;

export type CustomerSummaryType = typeof CustomerSummaryType[keyof typeof CustomerSummaryType];


export const CustomerSummaryType = {
  GUEST: 'GUEST',
  MEMBER: 'MEMBER',
} as const;

export interface CustomerSummary {
  name: string;
  /** @nullable */
  phone: string | null;
  type: CustomerSummaryType;
}

export interface AdminBookingResponse {
  arrears: boolean;
  balanceAmount: number;
  /** @nullable */
  balancePaidAt: string | null;
  balanceStatus: AdminBookingResponseBalanceStatus;
  bookingId: number;
  bookingNumber: string;
  className: string;
  customerSummary: CustomerSummary;
  depositAmount: number;
  /** @nullable */
  depositPaidAt: string | null;
  endAt: string;
  /**
     * @minimum 1
     * @maximum 8
     */
  participantCount: number;
  passBooking: boolean;
  source: AdminBookingResponseSource;
  startAt: string;
  status: AdminBookingResponseStatus;
}

export type CreateAdminBookingRequestSource = typeof CreateAdminBookingRequestSource[keyof typeof CreateAdminBookingRequestSource];


export const CreateAdminBookingRequestSource = {
  PHONE: 'PHONE',
  NAVER_TALK: 'NAVER_TALK',
  KAKAO: 'KAKAO',
  VISIT: 'VISIT',
} as const;

export interface CreateAdminBookingRequest {
  depositPaid: boolean;
  /**
     * @minLength 1
     * @maxLength 100
     */
  name: string;
  /**
     * @minimum 1
     * @maximum 8
     */
  participantCount: number;
  /**
     * @minLength 1
     * @maxLength 20
     */
  phone: string;
  slotId: number;
  source: CreateAdminBookingRequestSource;
}

export type BookingCancellationTaskResponseStatus = typeof BookingCancellationTaskResponseStatus[keyof typeof BookingCancellationTaskResponseStatus];


export const BookingCancellationTaskResponseStatus = {
  PENDING: 'PENDING',
  COMPLETED: 'COMPLETED',
} as const;

export type BookingCancellationTaskResponseType = typeof BookingCancellationTaskResponseType[keyof typeof BookingCancellationTaskResponseType];


export const BookingCancellationTaskResponseType = {
  BALANCE_SETTLEMENT: 'BALANCE_SETTLEMENT',
  MANUAL_COMPENSATION: 'MANUAL_COMPENSATION',
} as const;

export interface BookingCancellationTaskResponse {
  balanceAmount: number;
  bookingId: number;
  bookingNumber: string;
  className: string;
  compensationAmount: number;
  /** @nullable */
  completedAt: string | null;
  /** @nullable */
  completedByAdminId: number | null;
  createdAt: string;
  reason: string;
  startAt: string;
  status: BookingCancellationTaskResponseStatus;
  taskId: number;
  type: BookingCancellationTaskResponseType;
}

export interface BookingCancellationTaskCompletionResponse {
  changed: boolean;
  task: BookingCancellationTaskResponse;
}

export type AdminBookingSearchItemResponseBalanceStatus = typeof AdminBookingSearchItemResponseBalanceStatus[keyof typeof AdminBookingSearchItemResponseBalanceStatus];


export const AdminBookingSearchItemResponseBalanceStatus = {
  UNPAID: 'UNPAID',
  PAID: 'PAID',
} as const;

export type AdminBookingSearchItemResponseBookerType = typeof AdminBookingSearchItemResponseBookerType[keyof typeof AdminBookingSearchItemResponseBookerType];


export const AdminBookingSearchItemResponseBookerType = {
  GUEST: 'GUEST',
  MEMBER: 'MEMBER',
} as const;

export type AdminBookingSearchItemResponseSource = typeof AdminBookingSearchItemResponseSource[keyof typeof AdminBookingSearchItemResponseSource];


export const AdminBookingSearchItemResponseSource = {
  WEB: 'WEB',
  PHONE: 'PHONE',
  NAVER_TALK: 'NAVER_TALK',
  KAKAO: 'KAKAO',
  VISIT: 'VISIT',
} as const;

export type AdminBookingSearchItemResponseStatus = typeof AdminBookingSearchItemResponseStatus[keyof typeof AdminBookingSearchItemResponseStatus];


export const AdminBookingSearchItemResponseStatus = {
  BOOKED: 'BOOKED',
  CANCELED: 'CANCELED',
  NO_SHOW: 'NO_SHOW',
  COMPLETED: 'COMPLETED',
} as const;

export interface AdminBookingSearchItemResponse {
  arrears: boolean;
  balanceAmount: number;
  /** @nullable */
  balancePaidAt: string | null;
  balanceStatus: AdminBookingSearchItemResponseBalanceStatus;
  bookerName: string;
  /** @nullable */
  bookerPhone: string | null;
  bookerType: AdminBookingSearchItemResponseBookerType;
  bookingId: number;
  bookingNumber: string;
  className: string;
  createdAt: string;
  depositAmount: number;
  /** @nullable */
  depositPaidAt: string | null;
  endAt: string;
  participantCount: number;
  passBooking: boolean;
  source: AdminBookingSearchItemResponseSource;
  startAt: string;
  status: AdminBookingSearchItemResponseStatus;
}

export interface AdminBookingSearchPageResponse {
  content: AdminBookingSearchItemResponse[];
  page: number;
  size: number;
  totalCount: number;
  totalPages: number;
}

export interface UpdateBookingArrearsRequest {
  arrears: boolean;
}

export type BookingSettlementResponseBalanceStatus = typeof BookingSettlementResponseBalanceStatus[keyof typeof BookingSettlementResponseBalanceStatus];


export const BookingSettlementResponseBalanceStatus = {
  UNPAID: 'UNPAID',
  PAID: 'PAID',
} as const;

export type BookingSettlementResponseStatus = typeof BookingSettlementResponseStatus[keyof typeof BookingSettlementResponseStatus];


export const BookingSettlementResponseStatus = {
  BOOKED: 'BOOKED',
  COMPLETED: 'COMPLETED',
} as const;

export interface BookingSettlementResponse {
  arrears: boolean;
  /** @nullable */
  balancePaidAt: string | null;
  balanceStatus: BookingSettlementResponseBalanceStatus;
  bookingId: number;
  /**
     * @minimum 1
     * @maximum 8
     */
  participantCount: number;
  status: BookingSettlementResponseStatus;
}

export interface AdminBookingCancelRequest {
  /**
     * @minLength 0
     * @maxLength 200
     */
  reason: string;
}

/**
 * @nullable
 */
export type AdminBookingCancelResponseDepositRefundStatus = typeof AdminBookingCancelResponseDepositRefundStatus[keyof typeof AdminBookingCancelResponseDepositRefundStatus] | null;


export const AdminBookingCancelResponseDepositRefundStatus = {
  REQUESTED: 'REQUESTED',
  PROCESSING: 'PROCESSING',
  RETRYABLE: 'RETRYABLE',
  RECONCILIATION_REQUIRED: 'RECONCILIATION_REQUIRED',
  SUCCEEDED: 'SUCCEEDED',
  FAILED: 'FAILED',
} as const;

export type AdminBookingCancelResponseStatus = typeof AdminBookingCancelResponseStatus[keyof typeof AdminBookingCancelResponseStatus];


export const AdminBookingCancelResponseStatus = {
  CANCELED: 'CANCELED',
} as const;

export interface AdminBookingCancelResponse {
  balanceSettlementRequired: boolean;
  bookingId: number;
  depositRefundAmount: number;
  /** @nullable */
  depositRefundStatus: AdminBookingCancelResponseDepositRefundStatus;
  manualCompensationRequired: boolean;
  /**
     * @minimum 1
     * @maximum 8
     */
  participantCount: number;
  passCreditRestored: boolean;
  status: AdminBookingCancelResponseStatus;
}

export type BookingNoShowResponseStatus = typeof BookingNoShowResponseStatus[keyof typeof BookingNoShowResponseStatus];


export const BookingNoShowResponseStatus = {
  NO_SHOW: 'NO_SHOW',
} as const;

export interface BookingNoShowResponse {
  bookingId: number;
  /**
     * @minimum 1
     * @maximum 8
     */
  participantCount: number;
  status: BookingNoShowResponseStatus;
}

export type ListBookingsParams = {
date: string;
status?: ListBookingsStatus;
};

export type ListBookingsStatus = typeof ListBookingsStatus[keyof typeof ListBookingsStatus];


export const ListBookingsStatus = {
  BOOKED: 'BOOKED',
  CANCELED: 'CANCELED',
  NO_SHOW: 'NO_SHOW',
  COMPLETED: 'COMPLETED',
} as const;

export type SearchBookingsParams = {
status?: SearchBookingsStatus;
dateFrom?: string;
dateTo?: string;
keyword?: string;
page?: number;
size?: number;
};

export type SearchBookingsStatus = typeof SearchBookingsStatus[keyof typeof SearchBookingsStatus];


export const SearchBookingsStatus = {
  BOOKED: 'BOOKED',
  CANCELED: 'CANCELED',
  NO_SHOW: 'NO_SHOW',
  COMPLETED: 'COMPLETED',
} as const;

export const getListBookingsUrl = (params: ListBookingsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/bookings?${stringifiedParams}` : `/api/v1/admin/bookings`
}

export const listBookings = async (params: ListBookingsParams, options?: RequestInit): Promise<AdminBookingResponse[]> => {

  return generatedApiClient<AdminBookingResponse[]>(getListBookingsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCreateAdminBookingUrl = () => {




  return `/api/v1/admin/bookings`
}

export const createAdminBooking = async (createAdminBookingRequest: CreateAdminBookingRequest, options?: RequestInit): Promise<AdminBookingResponse> => {

  return generatedApiClient<AdminBookingResponse>(getCreateAdminBookingUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(createAdminBookingRequest)
  }
);}



export const getListPendingBookingCancellationTasksUrl = () => {




  return `/api/v1/admin/bookings/cancellation-tasks`
}

export const listPendingBookingCancellationTasks = async ( options?: RequestInit): Promise<BookingCancellationTaskResponse[]> => {

  return generatedApiClient<BookingCancellationTaskResponse[]>(getListPendingBookingCancellationTasksUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCompleteBookingCancellationTaskUrl = (taskId: number,) => {




  return `/api/v1/admin/bookings/cancellation-tasks/${taskId}/complete`
}

export const completeBookingCancellationTask = async (taskId: number, options?: RequestInit): Promise<BookingCancellationTaskCompletionResponse> => {

  return generatedApiClient<BookingCancellationTaskCompletionResponse>(getCompleteBookingCancellationTaskUrl(taskId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getSearchBookingsUrl = (params?: SearchBookingsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/bookings/search?${stringifiedParams}` : `/api/v1/admin/bookings/search`
}

export const searchBookings = async (params?: SearchBookingsParams, options?: RequestInit): Promise<AdminBookingSearchPageResponse> => {

  return generatedApiClient<AdminBookingSearchPageResponse>(getSearchBookingsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getUpdateArrearsUrl = (bookingId: number,) => {




  return `/api/v1/admin/bookings/${bookingId}/arrears`
}

export const updateArrears = async (bookingId: number,
    updateBookingArrearsRequest: UpdateBookingArrearsRequest, options?: RequestInit): Promise<BookingSettlementResponse> => {

  return generatedApiClient<BookingSettlementResponse>(getUpdateArrearsUrl(bookingId),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateBookingArrearsRequest)
  }
);}



export const getMarkBalancePaidUrl = (bookingId: number,) => {




  return `/api/v1/admin/bookings/${bookingId}/balance-payment`
}

export const markBalancePaid = async (bookingId: number, options?: RequestInit): Promise<BookingSettlementResponse> => {

  return generatedApiClient<BookingSettlementResponse>(getMarkBalancePaidUrl(bookingId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getCancelAdminBookingUrl = (bookingId: number,) => {




  return `/api/v1/admin/bookings/${bookingId}/cancel`
}

export const cancelAdminBooking = async (bookingId: number,
    adminBookingCancelRequest: AdminBookingCancelRequest, options?: RequestInit): Promise<AdminBookingCancelResponse> => {

  return generatedApiClient<AdminBookingCancelResponse>(getCancelAdminBookingUrl(bookingId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(adminBookingCancelRequest)
  }
);}



export const getCompleteUrl = (bookingId: number,) => {




  return `/api/v1/admin/bookings/${bookingId}/complete`
}

export const complete = async (bookingId: number, options?: RequestInit): Promise<BookingSettlementResponse> => {

  return generatedApiClient<BookingSettlementResponse>(getCompleteUrl(bookingId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getMarkNoShowUrl = (bookingId: number,) => {




  return `/api/v1/admin/bookings/${bookingId}/no-show`
}

export const markNoShow = async (bookingId: number, options?: RequestInit): Promise<BookingNoShowResponse> => {

  return generatedApiClient<BookingNoShowResponse>(getMarkNoShowUrl(bookingId),
  {
    ...options,
    method: 'POST'


  }
);}
