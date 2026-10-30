import { useState } from "react";
import { Alert, Button, Card } from "react-bootstrap";
import { Link } from "react-router";
import type { MemberReviewResponse } from "./api";
import { ConfirmModal, ErrorAlert } from "@/shared/ui";
import { ReviewDate, ReviewStars, ReviewStatusBadge } from "./ReviewDisplay";
import { ReviewForm } from "./ReviewForm";
import { ReviewImageUploader } from "./ReviewImageUploader";
import { ReviewOfficialReply } from "./ReviewOfficialReply";
import { ReviewTrustBadges } from "./ReviewTrustBadges";
import { isReviewContentChangedError } from "./reviewMutationConflict";
import { useReviewFormTriggerFocus } from "./useReviewFormFocus";
import { useDeleteReview, useUpdateReview } from "./useReviewMutations";
import { reviewTargetHref } from "./reviewTarget";

export function MemberReviewCard({ review }: { review: MemberReviewResponse }) {
  const [editing, setEditing] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const updateMutation = useUpdateReview(() => setEditing(false));
  const rememberEditingTrigger = useReviewFormTriggerFocus(editing);
  const deleteMutation = useDeleteReview();
  const revisionConflict = isReviewContentChangedError(updateMutation.error);
  const targetHref = reviewTargetHref(review.targetType, review.targetId);

  return (
    <Card className="review-card member-review-card">
      <Card.Body>
        {/* 배지 줄이 길어도(수정됨) 수정·삭제 버튼이 다음 줄로 내려가지 않도록 왼쪽 칸만 줄여 감싼다. */}
        <div className="d-flex justify-content-between align-items-start gap-3 mb-3">
          <div className="flex-grow-1" style={{ minWidth: 0 }}>
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
              {review.targetType === "PRODUCT" ? "작품" : "클래스"} · <ReviewDate value={review.createdAt} />
            </div>
          </div>
          {!editing && (
            <div className="d-flex flex-shrink-0 gap-2">
              <Button
                id={`member-review-edit-${review.id}`}
                type="button"
                size="sm"
                variant="outline-dark"
                onClick={(event) => {
                  rememberEditingTrigger(event.currentTarget);
                  setEditing(true);
                }}
              >
                수정
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline-dark"
                aria-haspopup="dialog"
                onClick={() => setConfirmingDelete(true)}
              >
                삭제
              </Button>
            </div>
          )}
        </div>

        {review.status === "HIDDEN" && (
          <Alert variant="warning" className="small">
            <strong className="d-block mb-1">공방에서 비공개 처리한 후기입니다.</strong>
            {review.hiddenReason && <span>사유: {review.hiddenReason}</span>}
            <span className="d-block mt-1">내용을 수정해도 자동으로 다시 공개되지 않습니다.</span>
          </Alert>
        )}

        {editing ? (
          <>
            {revisionConflict && (
              <Alert variant="warning" className="small">
                다른 화면에서 후기가 변경되었습니다. 최신 별점과 내용을 다시 확인한 뒤 저장해 주세요.
              </Alert>
            )}
            <ReviewForm
              initialRating={review.rating}
              initialContent={review.content}
              submitLabel="수정 저장"
              pending={updateMutation.isPending}
              error={revisionConflict ? undefined : updateMutation.error}
              hiddenNotice={review.status === "HIDDEN"}
              onCancel={() => {
                updateMutation.reset();
                setEditing(false);
              }}
              onSubmit={(value) => updateMutation.mutate({
                reviewId: review.id,
                expectedContentRevision: review.contentRevision,
                ...value,
              })}
            />
          </>
        ) : (
          review.content
            ? <p className="review-content mb-0">{review.content}</p>
            : <p className="text-muted-soft small mb-0">작성한 내용 없이 별점만 남긴 후기입니다.</p>
        )}

        {!editing && (
          <>
            <ReviewImageUploader review={review} />
            <ReviewOfficialReply reply={review.officialReply} />
          </>
        )}

        {/* 같은 카드의 사진 삭제(ReviewImageUploader)처럼 확인 창에서 되묻는다. */}
        <ConfirmModal
          show={confirmingDelete}
          title="후기 삭제"
          pending={deleteMutation.isPending}
          onClose={() => {
            deleteMutation.reset();
            setConfirmingDelete(false);
          }}
          confirmLabel={deleteMutation.isPending ? "삭제 중..." : "후기 삭제"}
          confirmVariant="dark"
          onConfirm={() => deleteMutation.mutate(review)}
          closeLabel="닫기"
          cancelLabel="취소"
        >
          <p className="mb-2">{review.targetName} 후기를 삭제할까요? 삭제한 후기는 복구할 수 없습니다.</p>
          <p className="small text-muted-soft mb-0">
            공방에서 비공개 처리한 후기는 삭제해도 같은 구매 건이나 예약에 다시 쓸 수 없습니다. 그 외 후기는 이용 완료 내역에서 다시 작성할 수 있습니다.
          </p>
          <div className="mt-3"><ErrorAlert error={deleteMutation.error} /></div>
        </ConfirmModal>
      </Card.Body>
    </Card>
  );
}
