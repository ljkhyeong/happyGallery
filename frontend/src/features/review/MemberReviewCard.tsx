import { useState } from "react";
import { Alert, Button, Card } from "react-bootstrap";
import { Link } from "react-router";
import type { MemberReviewResponse } from "./api";
import { ErrorAlert } from "@/shared/ui";
import { ReviewDate, ReviewStars, ReviewStatusBadge } from "./ReviewDisplay";
import { ReviewForm } from "./ReviewForm";
import { ReviewImageUploader } from "./ReviewImageUploader";
import { ReviewOfficialReply } from "./ReviewOfficialReply";
import { ReviewTrustBadges } from "./ReviewTrustBadges";
import { useDeleteReview, useUpdateReview } from "./useReviewMutations";

export function MemberReviewCard({ review }: { review: MemberReviewResponse }) {
  const [editing, setEditing] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const updateMutation = useUpdateReview(() => setEditing(false));
  const deleteMutation = useDeleteReview();
  const targetHref = review.targetType === "PRODUCT"
    ? `/products/${review.targetId}`
    : `/classes/${review.targetId}`;

  return (
    <Card className="review-card member-review-card">
      <Card.Body>
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-3">
          <div>
            <div className="d-flex flex-wrap align-items-center gap-2 mb-2">
              <ReviewStatusBadge status={review.status} />
              <ReviewStars rating={review.rating} />
              <ReviewTrustBadges
                verifiedTransaction={review.verifiedTransaction}
                edited={review.edited}
              />
            </div>
            <Link to={targetHref} className="fw-semibold text-decoration-none">
              {review.targetName}
            </Link>
            <div className="text-muted-soft small mt-1">
              {review.targetType === "PRODUCT" ? "상품" : "클래스"} · <ReviewDate value={review.createdAt} />
            </div>
          </div>
          {!editing && !confirmingDelete && (
            <div className="d-flex gap-2">
              <Button type="button" size="sm" variant="outline-dark" onClick={() => setEditing(true)}>
                수정
              </Button>
              <Button type="button" size="sm" variant="outline-danger" onClick={() => setConfirmingDelete(true)}>
                삭제
              </Button>
            </div>
          )}
        </div>

        {review.status === "HIDDEN" && (
          <Alert variant="warning" className="small">
            <strong className="d-block mb-1">관리자에 의해 숨겨진 후기입니다.</strong>
            {review.hiddenReason && <span>사유: {review.hiddenReason}</span>}
            <span className="d-block mt-1">내용을 수정해도 자동으로 다시 공개되지 않습니다.</span>
          </Alert>
        )}

        {editing ? (
          <ReviewForm
            initialRating={review.rating}
            initialContent={review.content}
            submitLabel="수정 저장"
            pending={updateMutation.isPending}
            error={updateMutation.error}
            hiddenNotice={review.status === "HIDDEN"}
            onCancel={() => {
              updateMutation.reset();
              setEditing(false);
            }}
            onSubmit={(value) => updateMutation.mutate({ reviewId: review.id, ...value })}
          />
        ) : (
          review.content
            ? <p className="review-content mb-0">{review.content}</p>
            : <p className="text-muted-soft small mb-0">작성한 내용 없이 별점만 남긴 후기입니다.</p>
        )}

        {!editing && !confirmingDelete && (
          <>
            <ReviewImageUploader review={review} />
            <ReviewOfficialReply reply={review.officialReply} />
          </>
        )}

        {confirmingDelete && (
          <Alert variant="danger" className="mb-0">
            <p className="mb-2">이 후기를 삭제할까요? 삭제한 후기는 복구할 수 없습니다.</p>
            <p className="small mb-2">
              일반 후기는 완료 내역에서 다시 작성할 수 있지만, 운영 정책으로 숨김 처리된 이력이 있으면 삭제 후에도 같은 이용 건으로 다시 작성할 수 없습니다.
            </p>
            <div className="d-flex gap-2">
              <Button
                type="button"
                size="sm"
                variant="danger"
                disabled={deleteMutation.isPending}
                onClick={() => deleteMutation.mutate(review)}
              >
                {deleteMutation.isPending ? "삭제 중..." : "삭제"}
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline-secondary"
                disabled={deleteMutation.isPending}
                onClick={() => {
                  deleteMutation.reset();
                  setConfirmingDelete(false);
                }}
              >
                취소
              </Button>
            </div>
            <div className="mt-2"><ErrorAlert error={deleteMutation.error} /></div>
          </Alert>
        )}
      </Card.Body>
    </Card>
  );
}
