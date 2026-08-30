import {
  addMyReviewImage,
  createClassReview,
  createProductReview,
  deleteMyReview,
  deleteMyReviewImage,
  getClassReviewCreationState,
  getMyReviewImage,
  getProductReviewCreationState,
  listClassReviews,
  listMyBookingReviews,
  listMyOrderReviews,
  listMyReviewOpportunities,
  listMyReviewReactions,
  listMyReviews,
  listProductReviews,
  markReviewHelpful,
  reportReview,
  unmarkReviewHelpful,
  updateMyReview,
  type CreateReviewReportRequest,
  type CreateClassReviewRequest,
  type CreateProductReviewRequest,
  type ListProductReviewsParams,
  type MemberReviewPageResponse,
  type MemberReviewReportResponse,
  type MemberReviewResponse,
  type PublicReviewPageResponse,
  type ReviewCreationStateResponse,
  type ReviewHelpfulResponse,
  type ReviewImageResponse,
  type ReviewOpportunityPageResponse,
  type ReviewOpportunityResponse,
  type ReviewReactionResponse,
  type UpdateReviewRequest,
} from "@/generated/api/review";

const PAGE_SIZE = 10;

export type ReviewSort = NonNullable<ListProductReviewsParams["sort"]>;
export type ReviewReportReason = CreateReviewReportRequest["reason"];

export type {
  MemberReviewResponse,
  PublicReviewPageResponse,
  ReviewCreationStateResponse,
  ReviewImageResponse,
  ReviewOpportunityResponse,
  ReviewReactionResponse,
};

interface PublicReviewFilters {
  rating?: number;
  sort: ReviewSort;
}

export function fetchProductReviews(
  productId: number,
  filters: PublicReviewFilters,
  cursor?: string,
  signal?: AbortSignal,
): Promise<PublicReviewPageResponse> {
  return listProductReviews(productId, { ...filters, cursor, size: PAGE_SIZE }, { signal });
}

export function fetchClassReviews(
  classId: number,
  filters: PublicReviewFilters,
  cursor?: string,
  signal?: AbortSignal,
): Promise<PublicReviewPageResponse> {
  return listClassReviews(classId, { ...filters, cursor, size: PAGE_SIZE }, { signal });
}

export function fetchMyReviews(
  cursor?: string,
  signal?: AbortSignal,
): Promise<MemberReviewPageResponse> {
  return listMyReviews({ cursor, size: PAGE_SIZE }, { signal });
}

export function fetchMyOrderReviews(
  orderId: number,
  signal?: AbortSignal,
): Promise<MemberReviewResponse[]> {
  return listMyOrderReviews(orderId, { signal });
}

export function fetchMyBookingReviews(
  bookingId: number,
  signal?: AbortSignal,
): Promise<MemberReviewResponse[]> {
  return listMyBookingReviews(bookingId, { signal });
}

export function fetchMyReviewOpportunities(
  cursor?: string,
  signal?: AbortSignal,
): Promise<ReviewOpportunityPageResponse> {
  return listMyReviewOpportunities({ cursor, size: PAGE_SIZE }, { signal });
}

export function fetchProductReviewCreationState(
  orderItemId: number,
  signal?: AbortSignal,
): Promise<ReviewCreationStateResponse> {
  return getProductReviewCreationState(orderItemId, { signal });
}

export function fetchClassReviewCreationState(
  bookingId: number,
  signal?: AbortSignal,
): Promise<ReviewCreationStateResponse> {
  return getClassReviewCreationState(bookingId, { signal });
}

export function fetchMyReviewReactions(
  reviewIds: number[],
  signal?: AbortSignal,
): Promise<ReviewReactionResponse[]> {
  return listMyReviewReactions({ reviewIds }, { signal });
}

export function submitProductReview(
  body: CreateProductReviewRequest,
): Promise<MemberReviewResponse> {
  return createProductReview(body);
}

export function submitClassReview(
  body: CreateClassReviewRequest,
): Promise<MemberReviewResponse> {
  return createClassReview(body);
}

export function editMyReview(
  reviewId: number,
  body: UpdateReviewRequest,
): Promise<MemberReviewResponse> {
  return updateMyReview(reviewId, body);
}

export function removeMyReview(reviewId: number): Promise<void> {
  return deleteMyReview(reviewId);
}

export function setReviewHelpful(
  reviewId: number,
  helpful: boolean,
): Promise<ReviewHelpfulResponse> {
  return helpful ? markReviewHelpful(reviewId) : unmarkReviewHelpful(reviewId);
}

export function submitReviewReport(
  reviewId: number,
  body: CreateReviewReportRequest,
): Promise<MemberReviewReportResponse> {
  return reportReview(reviewId, body);
}

export function uploadReviewImage(
  reviewId: number,
  file: File,
): Promise<ReviewImageResponse> {
  return addMyReviewImage(reviewId, { file });
}

export function removeReviewImage(reviewId: number, imageId: number): Promise<void> {
  return deleteMyReviewImage(reviewId, imageId);
}

export async function fetchMyReviewImage(
  reviewId: number,
  imageId: number,
  signal?: AbortSignal,
): Promise<Blob> {
  const blob = await getMyReviewImage(reviewId, imageId, {
    cache: "no-store",
    signal,
  });
  if (!blob.type.startsWith("image/")) {
    throw new Error("비공개 후기 사진을 불러오지 못했습니다. 다시 시도해 주세요.");
  }
  return blob;
}
