import { Badge, Card } from "react-bootstrap";
import type { PublicReviewResponse, ReviewReactionResponse } from "@/generated/api/review";
import { ReviewDate, ReviewStars } from "./ReviewDisplay";
import { ReviewHelpfulButton } from "./ReviewHelpfulButton";
import { ReviewImageGallery } from "./ReviewImageGallery";
import { ReviewOfficialReply } from "./ReviewOfficialReply";
import { ReviewReportModal } from "./ReviewReportModal";
import { ReviewTrustBadges } from "./ReviewTrustBadges";

interface Props {
  review: PublicReviewResponse;
  reaction?: ReviewReactionResponse;
  reactionLoading: boolean;
  isAuthenticated: boolean;
  loginHref: string;
  targetType: "PRODUCT" | "CLASS";
  targetId: number;
}

export function PublicReviewCard({
  review,
  reaction,
  reactionLoading,
  isAuthenticated,
  loginHref,
  targetType,
  targetId,
}: Props) {
  const interactionDescriptionId = reaction?.canInteract === false
    ? `review-interaction-description-${review.id}`
    : undefined;

  return (
    <Card className="review-card public-review-card">
      <Card.Body>
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-2">
          <div>
            <ReviewStars rating={review.rating} />
            <div className="mt-2">
              <ReviewTrustBadges
                verifiedTransaction={review.verifiedTransaction}
                edited={review.edited}
              />
            </div>
          </div>
          <small className="text-muted-soft"><ReviewDate value={review.createdAt} /></small>
        </div>
        <p className="review-content mb-3">{review.content}</p>
        <ReviewImageGallery images={review.images} />
        <ReviewOfficialReply reply={review.officialReply} />
        <footer className="review-card-footer">
          <div className="d-flex flex-wrap align-items-center gap-2">
            <small className="text-muted-soft">{review.authorName}</small>
            {reaction?.ownedByMe && <Badge bg="light" text="dark">내 후기</Badge>}
            {interactionDescriptionId && (
              <span id={interactionDescriptionId} className="visually-hidden">
                {reaction?.ownedByMe
                  ? "본인이 작성한 후기에는 도움돼요 또는 신고를 할 수 없습니다."
                  : "현재 이 후기에는 도움돼요 또는 신고를 할 수 없습니다."}
              </span>
            )}
          </div>
          <div className="review-reactions">
            <ReviewHelpfulButton
              reviewId={review.id}
              helpfulCount={review.helpfulCount}
              helpfulByMe={reaction?.helpfulByMe}
              canInteract={reaction?.canInteract}
              reactionLoading={reactionLoading}
              isAuthenticated={isAuthenticated}
              loginHref={loginHref}
              targetType={targetType}
              targetId={targetId}
              interactionDescriptionId={interactionDescriptionId}
            />
            <ReviewReportModal
              reviewId={review.id}
              reportedByMe={reaction?.reportedByMe}
              canInteract={reaction?.canInteract}
              reactionLoading={reactionLoading}
              isAuthenticated={isAuthenticated}
              loginHref={loginHref}
              interactionDescriptionId={interactionDescriptionId}
            />
          </div>
        </footer>
      </Card.Body>
    </Card>
  );
}
