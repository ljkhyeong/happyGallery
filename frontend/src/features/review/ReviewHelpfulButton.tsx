import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ThumbsUp } from "lucide-react";
import { Button } from "react-bootstrap";
import { Link } from "react-router";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert } from "@/shared/ui";
import { setReviewHelpful } from "./api";

interface Props {
  reviewId: number;
  helpfulCount: number;
  helpfulByMe?: boolean;
  reactionLoading: boolean;
  isAuthenticated: boolean;
  loginHref: string;
  targetType: "PRODUCT" | "CLASS";
  targetId: number;
}

export function ReviewHelpfulButton({
  reviewId,
  helpfulCount,
  helpfulByMe,
  reactionLoading,
  isAuthenticated,
  loginHref,
  targetType,
  targetId,
}: Props) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: (nextHelpful: boolean) => runForCurrentCustomer(
      () => setReviewHelpful(reviewId, nextHelpful),
      async (_, requireCurrent) => {
        requireCurrent();
        const publicKey = targetType === "PRODUCT"
          ? queryKeys.reviews.products.byProduct(targetId)
          : queryKeys.reviews.classes.byClass(targetId);
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: publicKey }),
          queryClient.invalidateQueries({ queryKey: queryKeys.member.reviews.all }),
          queryClient.invalidateQueries({ queryKey: queryKeys.admin.reviews.all }),
        ]);
        requireCurrent();
      },
    ),
  });

  if (!isAuthenticated) {
    return (
      <Link to={loginHref} className="btn btn-sm btn-outline-secondary review-reaction-button">
        <ThumbsUp size={15} aria-hidden="true" /> 로그인 후 도움돼요 {helpfulCount.toLocaleString("ko-KR")}
      </Link>
    );
  }

  return (
    <div>
      <Button
        type="button"
        size="sm"
        variant={helpfulByMe ? "dark" : "outline-secondary"}
        className="review-reaction-button"
        aria-pressed={helpfulByMe === true}
        disabled={reactionLoading || helpfulByMe === undefined || mutation.isPending}
        onClick={() => mutation.mutate(!helpfulByMe)}
      >
        <ThumbsUp size={15} aria-hidden="true" />
        {mutation.isPending ? "반영 중..." : `도움돼요 ${helpfulCount.toLocaleString("ko-KR")}`}
      </Button>
      <div className="mt-2"><ErrorAlert error={mutation.error} /></div>
    </div>
  );
}
