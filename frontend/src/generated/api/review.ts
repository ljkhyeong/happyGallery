import { generatedApiClient } from '../../shared/api/generatedClient';
export type AdminReviewReportResponseReason = typeof AdminReviewReportResponseReason[keyof typeof AdminReviewReportResponseReason];


export const AdminReviewReportResponseReason = {
  SPAM: 'SPAM',
  ABUSIVE: 'ABUSIVE',
  PRIVACY: 'PRIVACY',
  FALSE_INFORMATION: 'FALSE_INFORMATION',
  OTHER: 'OTHER',
} as const;

export type AdminReviewReportResponseSnapshotStatus = typeof AdminReviewReportResponseSnapshotStatus[keyof typeof AdminReviewReportResponseSnapshotStatus];


export const AdminReviewReportResponseSnapshotStatus = {
  PUBLISHED: 'PUBLISHED',
  HIDDEN: 'HIDDEN',
} as const;

export type AdminReviewReportResponseStatus = typeof AdminReviewReportResponseStatus[keyof typeof AdminReviewReportResponseStatus];


export const AdminReviewReportResponseStatus = {
  PENDING: 'PENDING',
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED',
} as const;

export interface AdminReviewReportResponse {
  createdAt: string;
  /** @nullable */
  decidedAt: string | null;
  /** @nullable */
  decidedByAdminId: number | null;
  /** @nullable */
  decisionNote: string | null;
  /** @nullable */
  detail: string | null;
  id: number;
  reason: AdminReviewReportResponseReason;
  reporterUserId: number;
  reviewId: number;
  snapshotContent: string;
  /** @nullable */
  snapshotEditedAt: string | null;
  /**
     * @minimum 1
     * @maximum 5
     */
  snapshotRating: number;
  snapshotStatus: AdminReviewReportResponseSnapshotStatus;
  status: AdminReviewReportResponseStatus;
}

export interface AdminReviewReportPageResponse {
  content: AdminReviewReportResponse[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
}

export type DecideReviewReportRequestDecision = typeof DecideReviewReportRequestDecision[keyof typeof DecideReviewReportRequestDecision];


export const DecideReviewReportRequestDecision = {
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED',
} as const;

export interface DecideReviewReportRequest {
  decision: DecideReviewReportRequestDecision;
  /**
     * @minLength 0
     * @maxLength 1000
     * @nullable
     */
  note?: string | null;
}

export interface ReviewImageResponse {
  createdAt: string;
  id: number;
  imageUrl: string;
  /**
     * @minimum 0
     * @maximum 4
     */
  sortOrder: number;
}

export interface AdminOfficialReviewReplyResponse {
  adminUserId: number;
  content: string;
  createdAt: string;
  edited: boolean;
  /** @nullable */
  editedAt: string | null;
}

export type AdminReviewResponseSourceType = typeof AdminReviewResponseSourceType[keyof typeof AdminReviewResponseSourceType];


export const AdminReviewResponseSourceType = {
  ORDER_ITEM: 'ORDER_ITEM',
  BOOKING: 'BOOKING',
} as const;

export type AdminReviewResponseStatus = typeof AdminReviewResponseStatus[keyof typeof AdminReviewResponseStatus];


export const AdminReviewResponseStatus = {
  PUBLISHED: 'PUBLISHED',
  HIDDEN: 'HIDDEN',
} as const;

export type AdminReviewResponseTargetType = typeof AdminReviewResponseTargetType[keyof typeof AdminReviewResponseTargetType];


export const AdminReviewResponseTargetType = {
  PRODUCT: 'PRODUCT',
  CLASS: 'CLASS',
} as const;

export interface AdminReviewResponse {
  authorName: string;
  content: string;
  createdAt: string;
  edited: boolean;
  /** @nullable */
  editedAt: string | null;
  /** @minimum 0 */
  helpfulCount: number;
  /** @nullable */
  hiddenAt: string | null;
  /** @nullable */
  hiddenByAdminId: number | null;
  /** @nullable */
  hiddenReason: string | null;
  id: number;
  /** @maxItems 5 */
  images: ReviewImageResponse[];
  officialReply: AdminOfficialReviewReplyResponse | null;
  /**
     * @minimum 1
     * @maximum 5
     */
  rating: number;
  sourceId: number;
  sourceType: AdminReviewResponseSourceType;
  status: AdminReviewResponseStatus;
  targetId: number;
  targetName: string;
  targetType: AdminReviewResponseTargetType;
  updatedAt: string;
  userId: number;
  verifiedTransaction: boolean;
}

export interface AdminReviewPageResponse {
  content: AdminReviewResponse[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
}

export type ReviewModerationActionResponseAction = typeof ReviewModerationActionResponseAction[keyof typeof ReviewModerationActionResponseAction];


export const ReviewModerationActionResponseAction = {
  HIDE: 'HIDE',
  REPUBLISH: 'REPUBLISH',
} as const;

export type ReviewModerationActionResponseNewStatus = typeof ReviewModerationActionResponseNewStatus[keyof typeof ReviewModerationActionResponseNewStatus];


export const ReviewModerationActionResponseNewStatus = {
  PUBLISHED: 'PUBLISHED',
  HIDDEN: 'HIDDEN',
} as const;

export type ReviewModerationActionResponsePreviousStatus = typeof ReviewModerationActionResponsePreviousStatus[keyof typeof ReviewModerationActionResponsePreviousStatus];


export const ReviewModerationActionResponsePreviousStatus = {
  PUBLISHED: 'PUBLISHED',
  HIDDEN: 'HIDDEN',
} as const;

export interface ReviewModerationActionResponse {
  action: ReviewModerationActionResponseAction;
  adminUserId: number;
  createdAt: string;
  id: number;
  newStatus: ReviewModerationActionResponseNewStatus;
  previousStatus: ReviewModerationActionResponsePreviousStatus;
  /** @nullable */
  reason: string | null;
  reviewId: number;
}

export interface UpsertReviewReplyRequest {
  /**
     * @minLength 1
     * @maxLength 16000
     */
  content: string;
}

export type UpdateReviewStatusRequestStatus = typeof UpdateReviewStatusRequestStatus[keyof typeof UpdateReviewStatusRequestStatus];


export const UpdateReviewStatusRequestStatus = {
  PUBLISHED: 'PUBLISHED',
  HIDDEN: 'HIDDEN',
} as const;

export interface UpdateReviewStatusRequest {
  /**
     * HIDDEN 전환 시 필수이며 PUBLISHED 전환 시 무시됩니다.
     * @minLength 0
     * @maxLength 500
     * @nullable
     */
  reason?: string | null;
  status: UpdateReviewStatusRequestStatus;
}

export interface OfficialReviewReplyResponse {
  content: string;
  createdAt: string;
  edited: boolean;
  /** @nullable */
  editedAt: string | null;
}

export type PublicReviewResponseSourceType = typeof PublicReviewResponseSourceType[keyof typeof PublicReviewResponseSourceType];


export const PublicReviewResponseSourceType = {
  ORDER_ITEM: 'ORDER_ITEM',
  BOOKING: 'BOOKING',
} as const;

export interface PublicReviewResponse {
  authorName: string;
  content: string;
  createdAt: string;
  edited: boolean;
  /** @nullable */
  editedAt: string | null;
  /** @minimum 0 */
  helpfulCount: number;
  id: number;
  /** @maxItems 5 */
  images: ReviewImageResponse[];
  officialReply: OfficialReviewReplyResponse | null;
  /**
     * @minimum 1
     * @maximum 5
     */
  rating: number;
  sourceType: PublicReviewResponseSourceType;
  updatedAt: string;
  verifiedTransaction: boolean;
}

export interface ReviewRatingHistogramResponse {
  /** @minimum 0 */
  rating1: number;
  /** @minimum 0 */
  rating2: number;
  /** @minimum 0 */
  rating3: number;
  /** @minimum 0 */
  rating4: number;
  /** @minimum 0 */
  rating5: number;
}

export interface ReviewSummaryResponse {
  /**
     * @minimum 0
     * @maximum 5
     */
  averageRating: number;
  histogram: ReviewRatingHistogramResponse;
  /** @minimum 0 */
  reviewCount: number;
}

export interface PublicReviewPageResponse {
  content: PublicReviewResponse[];
  /** @minimum 0 */
  filteredCount: number;
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
  summary: ReviewSummaryResponse;
}

export type MemberReviewResponseSourceType = typeof MemberReviewResponseSourceType[keyof typeof MemberReviewResponseSourceType];


export const MemberReviewResponseSourceType = {
  ORDER_ITEM: 'ORDER_ITEM',
  BOOKING: 'BOOKING',
} as const;

export type MemberReviewResponseStatus = typeof MemberReviewResponseStatus[keyof typeof MemberReviewResponseStatus];


export const MemberReviewResponseStatus = {
  PUBLISHED: 'PUBLISHED',
  HIDDEN: 'HIDDEN',
} as const;

export type MemberReviewResponseTargetType = typeof MemberReviewResponseTargetType[keyof typeof MemberReviewResponseTargetType];


export const MemberReviewResponseTargetType = {
  PRODUCT: 'PRODUCT',
  CLASS: 'CLASS',
} as const;

export interface MemberReviewResponse {
  content: string;
  createdAt: string;
  edited: boolean;
  /** @nullable */
  editedAt: string | null;
  /** @minimum 0 */
  helpfulCount: number;
  /** @nullable */
  hiddenReason: string | null;
  id: number;
  /** @maxItems 5 */
  images: ReviewImageResponse[];
  officialReply: OfficialReviewReplyResponse | null;
  /**
     * @minimum 1
     * @maximum 5
     */
  rating: number;
  sourceId: number;
  sourceType: MemberReviewResponseSourceType;
  status: MemberReviewResponseStatus;
  targetId: number;
  targetName: string;
  targetType: MemberReviewResponseTargetType;
  updatedAt: string;
  verifiedTransaction: boolean;
}

export interface MemberReviewPageResponse {
  content: MemberReviewResponse[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
}

export interface CreateClassReviewRequest {
  bookingId: number;
  /**
     * @minLength 1
     * @maxLength 16000
     */
  content: string;
  /**
     * @minimum 1
     * @maximum 5
     */
  rating: number;
}

export type ReviewCreationStateResponseSourceType = typeof ReviewCreationStateResponseSourceType[keyof typeof ReviewCreationStateResponseSourceType];


export const ReviewCreationStateResponseSourceType = {
  ORDER_ITEM: 'ORDER_ITEM',
  BOOKING: 'BOOKING',
} as const;

export type ReviewCreationStateResponseStatus = typeof ReviewCreationStateResponseStatus[keyof typeof ReviewCreationStateResponseStatus];


export const ReviewCreationStateResponseStatus = {
  AVAILABLE: 'AVAILABLE',
  REVIEW_EXISTS: 'REVIEW_EXISTS',
  RECREATION_BLOCKED: 'RECREATION_BLOCKED',
  NOT_REVIEWABLE: 'NOT_REVIEWABLE',
} as const;

export type ReviewCreationStateResponseTargetType = typeof ReviewCreationStateResponseTargetType[keyof typeof ReviewCreationStateResponseTargetType];


export const ReviewCreationStateResponseTargetType = {
  PRODUCT: 'PRODUCT',
  CLASS: 'CLASS',
} as const;

export interface ReviewCreationStateResponse {
  sourceId: number;
  sourceType: ReviewCreationStateResponseSourceType;
  status: ReviewCreationStateResponseStatus;
  targetType: ReviewCreationStateResponseTargetType;
}

export type ReviewOpportunityResponseSourceType = typeof ReviewOpportunityResponseSourceType[keyof typeof ReviewOpportunityResponseSourceType];


export const ReviewOpportunityResponseSourceType = {
  ORDER_ITEM: 'ORDER_ITEM',
  BOOKING: 'BOOKING',
} as const;

export type ReviewOpportunityResponseTargetType = typeof ReviewOpportunityResponseTargetType[keyof typeof ReviewOpportunityResponseTargetType];


export const ReviewOpportunityResponseTargetType = {
  PRODUCT: 'PRODUCT',
  CLASS: 'CLASS',
} as const;

export interface ReviewOpportunityResponse {
  /** @nullable */
  bookingId: number | null;
  completedAt: string;
  /** @nullable */
  orderId: number | null;
  sourceId: number;
  sourceType: ReviewOpportunityResponseSourceType;
  targetId: number;
  targetName: string;
  targetType: ReviewOpportunityResponseTargetType;
}

export interface CreateProductReviewRequest {
  /**
     * @minLength 1
     * @maxLength 16000
     */
  content: string;
  orderItemId: number;
  /**
     * @minimum 1
     * @maximum 5
     */
  rating: number;
}

export interface ReviewReactionResponse {
  helpfulByMe: boolean;
  reportedByMe: boolean;
  reviewId: number;
}

export interface UpdateReviewRequest {
  /**
     * @minLength 1
     * @maxLength 16000
     */
  content: string;
  /**
     * @minimum 1
     * @maximum 5
     */
  rating: number;
}

export interface ReviewHelpfulResponse {
  helpfulByMe: boolean;
  /** @minimum 0 */
  helpfulCount: number;
  reviewId: number;
}

export type CreateReviewReportRequestReason = typeof CreateReviewReportRequestReason[keyof typeof CreateReviewReportRequestReason];


export const CreateReviewReportRequestReason = {
  SPAM: 'SPAM',
  ABUSIVE: 'ABUSIVE',
  PRIVACY: 'PRIVACY',
  FALSE_INFORMATION: 'FALSE_INFORMATION',
  OTHER: 'OTHER',
} as const;

export interface CreateReviewReportRequest {
  /**
     * @minLength 0
     * @maxLength 1000
     * @nullable
     */
  detail?: string | null;
  reason: CreateReviewReportRequestReason;
}

export type MemberReviewReportResponseReason = typeof MemberReviewReportResponseReason[keyof typeof MemberReviewReportResponseReason];


export const MemberReviewReportResponseReason = {
  SPAM: 'SPAM',
  ABUSIVE: 'ABUSIVE',
  PRIVACY: 'PRIVACY',
  FALSE_INFORMATION: 'FALSE_INFORMATION',
  OTHER: 'OTHER',
} as const;

export type MemberReviewReportResponseStatus = typeof MemberReviewReportResponseStatus[keyof typeof MemberReviewReportResponseStatus];


export const MemberReviewReportResponseStatus = {
  PENDING: 'PENDING',
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED',
} as const;

export interface MemberReviewReportResponse {
  createdAt: string;
  /** @nullable */
  detail: string | null;
  id: number;
  reason: MemberReviewReportResponseReason;
  reviewId: number;
  status: MemberReviewReportResponseStatus;
}

export type ListAdminReviewReportsParams = {
status?: ListAdminReviewReportsStatus;
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
};

export type ListAdminReviewReportsStatus = typeof ListAdminReviewReportsStatus[keyof typeof ListAdminReviewReportsStatus];


export const ListAdminReviewReportsStatus = {
  PENDING: 'PENDING',
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED',
} as const;

export type ListAdminReviewsParams = {
targetType?: ListAdminReviewsTargetType;
status?: ListAdminReviewsStatus;
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
};

export type ListAdminReviewsTargetType = typeof ListAdminReviewsTargetType[keyof typeof ListAdminReviewsTargetType];


export const ListAdminReviewsTargetType = {
  PRODUCT: 'PRODUCT',
  CLASS: 'CLASS',
} as const;

export type ListAdminReviewsStatus = typeof ListAdminReviewsStatus[keyof typeof ListAdminReviewsStatus];


export const ListAdminReviewsStatus = {
  PUBLISHED: 'PUBLISHED',
  HIDDEN: 'HIDDEN',
} as const;

export type ListClassReviewsParams = {
/**
 * @minimum 1
 * @maximum 5
 */
rating?: number;
sort?: ListClassReviewsSort;
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
};

export type ListClassReviewsSort = typeof ListClassReviewsSort[keyof typeof ListClassReviewsSort];


export const ListClassReviewsSort = {
  LATEST: 'LATEST',
  RATING_HIGH: 'RATING_HIGH',
  RATING_LOW: 'RATING_LOW',
} as const;

export type ListMyReviewsParams = {
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
};

export type ListMyReviewReactionsParams = {
/**
 * @minItems 1
 * @maxItems 100
 * @items.minimum 1
 */
reviewIds: number[];
};

export type AddMyReviewImageBody = {
  /** JPEG 또는 PNG 후기 이미지 */
  file: Blob;
};

export type ListProductReviewsParams = {
/**
 * @minimum 1
 * @maximum 5
 */
rating?: number;
sort?: ListProductReviewsSort;
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
};

export type ListProductReviewsSort = typeof ListProductReviewsSort[keyof typeof ListProductReviewsSort];


export const ListProductReviewsSort = {
  LATEST: 'LATEST',
  RATING_HIGH: 'RATING_HIGH',
  RATING_LOW: 'RATING_LOW',
} as const;

export const getListAdminReviewReportsUrl = (params?: ListAdminReviewReportsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/review-reports?${stringifiedParams}` : `/api/v1/admin/review-reports`
}

export const listAdminReviewReports = async (params?: ListAdminReviewReportsParams, options?: RequestInit): Promise<AdminReviewReportPageResponse> => {

  return generatedApiClient<AdminReviewReportPageResponse>(getListAdminReviewReportsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getDecideAdminReviewReportUrl = (reportId: number,) => {




  return `/api/v1/admin/review-reports/${reportId}`
}

export const decideAdminReviewReport = async (reportId: number,
    decideReviewReportRequest: DecideReviewReportRequest, options?: RequestInit): Promise<AdminReviewReportResponse> => {

  return generatedApiClient<AdminReviewReportResponse>(getDecideAdminReviewReportUrl(reportId),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(decideReviewReportRequest)
  }
);}



export const getListAdminReviewsUrl = (params?: ListAdminReviewsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/reviews?${stringifiedParams}` : `/api/v1/admin/reviews`
}

export const listAdminReviews = async (params?: ListAdminReviewsParams, options?: RequestInit): Promise<AdminReviewPageResponse> => {

  return generatedApiClient<AdminReviewPageResponse>(getListAdminReviewsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListReviewModerationActionsUrl = (reviewId: number,) => {




  return `/api/v1/admin/reviews/${reviewId}/moderation-actions`
}

export const listReviewModerationActions = async (reviewId: number, options?: RequestInit): Promise<ReviewModerationActionResponse[]> => {

  return generatedApiClient<ReviewModerationActionResponse[]>(getListReviewModerationActionsUrl(reviewId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getDeleteOfficialReviewReplyUrl = (reviewId: number,) => {




  return `/api/v1/admin/reviews/${reviewId}/reply`
}

export const deleteOfficialReviewReply = async (reviewId: number, options?: RequestInit): Promise<AdminReviewResponse> => {

  return generatedApiClient<AdminReviewResponse>(getDeleteOfficialReviewReplyUrl(reviewId),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getUpsertOfficialReviewReplyUrl = (reviewId: number,) => {




  return `/api/v1/admin/reviews/${reviewId}/reply`
}

export const upsertOfficialReviewReply = async (reviewId: number,
    upsertReviewReplyRequest: UpsertReviewReplyRequest, options?: RequestInit): Promise<AdminReviewResponse> => {

  return generatedApiClient<AdminReviewResponse>(getUpsertOfficialReviewReplyUrl(reviewId),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(upsertReviewReplyRequest)
  }
);}



export const getUpdateAdminReviewStatusUrl = (reviewId: number,) => {




  return `/api/v1/admin/reviews/${reviewId}/status`
}

export const updateAdminReviewStatus = async (reviewId: number,
    updateReviewStatusRequest: UpdateReviewStatusRequest, options?: RequestInit): Promise<AdminReviewResponse> => {

  return generatedApiClient<AdminReviewResponse>(getUpdateAdminReviewStatusUrl(reviewId),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateReviewStatusRequest)
  }
);}



export const getListClassReviewsUrl = (classId: number,
    params?: ListClassReviewsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/classes/${classId}/reviews?${stringifiedParams}` : `/api/v1/classes/${classId}/reviews`
}

export const listClassReviews = async (classId: number,
    params?: ListClassReviewsParams, options?: RequestInit): Promise<PublicReviewPageResponse> => {

  return generatedApiClient<PublicReviewPageResponse>(getListClassReviewsUrl(classId,params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListMyReviewsUrl = (params?: ListMyReviewsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/me/reviews?${stringifiedParams}` : `/api/v1/me/reviews`
}

export const listMyReviews = async (params?: ListMyReviewsParams, options?: RequestInit): Promise<MemberReviewPageResponse> => {

  return generatedApiClient<MemberReviewPageResponse>(getListMyReviewsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListMyBookingReviewsUrl = (bookingId: number,) => {




  return `/api/v1/me/reviews/bookings/${bookingId}`
}

export const listMyBookingReviews = async (bookingId: number, options?: RequestInit): Promise<MemberReviewResponse[]> => {

  return generatedApiClient<MemberReviewResponse[]>(getListMyBookingReviewsUrl(bookingId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCreateClassReviewUrl = () => {




  return `/api/v1/me/reviews/classes`
}

export const createClassReview = async (createClassReviewRequest: CreateClassReviewRequest, options?: RequestInit): Promise<MemberReviewResponse> => {

  return generatedApiClient<MemberReviewResponse>(getCreateClassReviewUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(createClassReviewRequest)
  }
);}



export const getGetClassReviewCreationStateUrl = (bookingId: number,) => {




  return `/api/v1/me/reviews/classes/${bookingId}/creation-state`
}

export const getClassReviewCreationState = async (bookingId: number, options?: RequestInit): Promise<ReviewCreationStateResponse> => {

  return generatedApiClient<ReviewCreationStateResponse>(getGetClassReviewCreationStateUrl(bookingId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListMyReviewOpportunitiesUrl = () => {




  return `/api/v1/me/reviews/opportunities`
}

export const listMyReviewOpportunities = async ( options?: RequestInit): Promise<ReviewOpportunityResponse[]> => {

  return generatedApiClient<ReviewOpportunityResponse[]>(getListMyReviewOpportunitiesUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListMyOrderReviewsUrl = (orderId: number,) => {




  return `/api/v1/me/reviews/orders/${orderId}`
}

export const listMyOrderReviews = async (orderId: number, options?: RequestInit): Promise<MemberReviewResponse[]> => {

  return generatedApiClient<MemberReviewResponse[]>(getListMyOrderReviewsUrl(orderId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCreateProductReviewUrl = () => {




  return `/api/v1/me/reviews/products`
}

export const createProductReview = async (createProductReviewRequest: CreateProductReviewRequest, options?: RequestInit): Promise<MemberReviewResponse> => {

  return generatedApiClient<MemberReviewResponse>(getCreateProductReviewUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(createProductReviewRequest)
  }
);}



export const getGetProductReviewCreationStateUrl = (orderItemId: number,) => {




  return `/api/v1/me/reviews/products/${orderItemId}/creation-state`
}

export const getProductReviewCreationState = async (orderItemId: number, options?: RequestInit): Promise<ReviewCreationStateResponse> => {

  return generatedApiClient<ReviewCreationStateResponse>(getGetProductReviewCreationStateUrl(orderItemId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListMyReviewReactionsUrl = (params: ListMyReviewReactionsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {
    const explodeParameters = ["reviewIds"];

    if (Array.isArray(value) && explodeParameters.includes(key)) {
      value.forEach((v) => {
        normalizedParams.append(key, v === null ? 'null' : String(v));
      });
      return;
    }


  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/me/reviews/reactions?${stringifiedParams}` : `/api/v1/me/reviews/reactions`
}

export const listMyReviewReactions = async (params: ListMyReviewReactionsParams, options?: RequestInit): Promise<ReviewReactionResponse[]> => {

  return generatedApiClient<ReviewReactionResponse[]>(getListMyReviewReactionsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getDeleteMyReviewUrl = (reviewId: number,) => {




  return `/api/v1/me/reviews/${reviewId}`
}

export const deleteMyReview = async (reviewId: number, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getDeleteMyReviewUrl(reviewId),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getUpdateMyReviewUrl = (reviewId: number,) => {




  return `/api/v1/me/reviews/${reviewId}`
}

export const updateMyReview = async (reviewId: number,
    updateReviewRequest: UpdateReviewRequest, options?: RequestInit): Promise<MemberReviewResponse> => {

  return generatedApiClient<MemberReviewResponse>(getUpdateMyReviewUrl(reviewId),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateReviewRequest)
  }
);}



export const getUnmarkReviewHelpfulUrl = (reviewId: number,) => {




  return `/api/v1/me/reviews/${reviewId}/helpful`
}

export const unmarkReviewHelpful = async (reviewId: number, options?: RequestInit): Promise<ReviewHelpfulResponse> => {

  return generatedApiClient<ReviewHelpfulResponse>(getUnmarkReviewHelpfulUrl(reviewId),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getMarkReviewHelpfulUrl = (reviewId: number,) => {




  return `/api/v1/me/reviews/${reviewId}/helpful`
}

export const markReviewHelpful = async (reviewId: number, options?: RequestInit): Promise<ReviewHelpfulResponse> => {

  return generatedApiClient<ReviewHelpfulResponse>(getMarkReviewHelpfulUrl(reviewId),
  {
    ...options,
    method: 'PUT'


  }
);}



export const getAddMyReviewImageUrl = (reviewId: number,) => {




  return `/api/v1/me/reviews/${reviewId}/images`
}

export const addMyReviewImage = async (reviewId: number,
    addMyReviewImageBody: AddMyReviewImageBody, options?: RequestInit): Promise<ReviewImageResponse> => {
    const formData = new FormData();
formData.append(`file`, addMyReviewImageBody.file);

  return generatedApiClient<ReviewImageResponse>(getAddMyReviewImageUrl(reviewId),
  {
    ...options,
    method: 'POST'
    ,
    body: formData
  }
);}



export const getDeleteMyReviewImageUrl = (reviewId: number,
    imageId: number,) => {




  return `/api/v1/me/reviews/${reviewId}/images/${imageId}`
}

export const deleteMyReviewImage = async (reviewId: number,
    imageId: number, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getDeleteMyReviewImageUrl(reviewId,imageId),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getReportReviewUrl = (reviewId: number,) => {




  return `/api/v1/me/reviews/${reviewId}/reports`
}

export const reportReview = async (reviewId: number,
    createReviewReportRequest: CreateReviewReportRequest, options?: RequestInit): Promise<MemberReviewReportResponse> => {

  return generatedApiClient<MemberReviewReportResponse>(getReportReviewUrl(reviewId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(createReviewReportRequest)
  }
);}



export const getListProductReviewsUrl = (productId: number,
    params?: ListProductReviewsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/products/${productId}/reviews?${stringifiedParams}` : `/api/v1/products/${productId}/reviews`
}

export const listProductReviews = async (productId: number,
    params?: ListProductReviewsParams, options?: RequestInit): Promise<PublicReviewPageResponse> => {

  return generatedApiClient<PublicReviewPageResponse>(getListProductReviewsUrl(productId,params),
  {
    ...options,
    method: 'GET'


  }
);}
