import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { MemberReviewResponse } from "@/generated/api/review";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { useToast } from "@/shared/ui";
import {
  editMyReview,
  removeMyReview,
  submitClassReview,
  submitProductReview,
} from "./api";

function useReviewMutationSuccess() {
  const queryClient = useQueryClient();
  const toast = useToast();

  return async (
    review: MemberReviewResponse,
    message: string,
    requireCurrent: () => void,
    onApplied?: () => void,
  ) => {
    requireCurrent();
    const publicKey = review.targetType === "PRODUCT"
      ? queryKeys.reviews.products.byProduct(review.targetId)
      : queryKeys.reviews.classes.byClass(review.targetId);

    await Promise.all([
      queryClient.invalidateQueries({ queryKey: publicKey }),
      queryClient.invalidateQueries({ queryKey: queryKeys.member.reviews.all }),
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.reviews.all }),
    ]);
    requireCurrent();
    toast.show(message);
    requireCurrent();
    onApplied?.();
    requireCurrent();
    return review;
  };
}

export function useCreateProductReview(onApplied?: () => void) {
  const applySuccess = useReviewMutationSuccess();
  return useMutation({
    mutationFn: (input: { orderItemId: number; rating: number; content: string }) =>
      runForCurrentCustomer(
        () => submitProductReview(input),
        (review, requireCurrent) => applySuccess(
          review,
          "상품 후기를 등록했습니다.",
          requireCurrent,
          onApplied,
        ),
      ),
  });
}

export function useCreateClassReview(onApplied?: () => void) {
  const applySuccess = useReviewMutationSuccess();
  return useMutation({
    mutationFn: (input: { bookingId: number; rating: number; content: string }) =>
      runForCurrentCustomer(
        () => submitClassReview(input),
        (review, requireCurrent) => applySuccess(
          review,
          "클래스 후기를 등록했습니다.",
          requireCurrent,
          onApplied,
        ),
      ),
  });
}

export function useUpdateReview(onApplied?: () => void) {
  const applySuccess = useReviewMutationSuccess();
  return useMutation({
    mutationFn: (input: { reviewId: number; rating: number; content: string }) =>
      runForCurrentCustomer(
        () => editMyReview(input.reviewId, {
          rating: input.rating,
          content: input.content,
        }),
        (review, requireCurrent) => applySuccess(
          review,
          "후기를 수정했습니다.",
          requireCurrent,
          onApplied,
        ),
      ),
  });
}

export function useDeleteReview() {
  const applySuccess = useReviewMutationSuccess();
  return useMutation({
    mutationFn: (review: MemberReviewResponse) =>
      runForCurrentCustomer(
        () => removeMyReview(review.id),
        (_, requireCurrent) => applySuccess(
          review,
          "후기를 삭제했습니다.",
          requireCurrent,
        ),
      ),
  });
}
