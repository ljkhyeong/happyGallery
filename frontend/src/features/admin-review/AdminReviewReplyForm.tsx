import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button, Form } from "react-bootstrap";
import type { AdminReviewResponse } from "./api";
import { ReviewOfficialReply } from "@/features/review/ReviewOfficialReply";
import { queryKeys } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { ErrorAlert, useToast } from "@/shared/ui";
import { removeOfficialReviewReply, saveOfficialReviewReply } from "./api";

interface Props {
  review: AdminReviewResponse;
  adminKey: string;
  onAuthError: () => void;
}

export function AdminReviewReplyForm({ review, adminKey, onAuthError }: Props) {
  const [editing, setEditing] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [content, setContent] = useState(review.officialReply?.content ?? "");
  const queryClient = useQueryClient();
  const toast = useToast();

  const invalidate = async (updated: AdminReviewResponse) => {
    const publicKey = updated.targetType === "PRODUCT"
      ? queryKeys.reviews.products.byProduct(updated.targetId)
      : queryKeys.reviews.classes.byClass(updated.targetId);
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.reviews.all }),
      queryClient.invalidateQueries({ queryKey: queryKeys.member.reviews.all }),
      queryClient.invalidateQueries({ queryKey: publicKey }),
    ]);
  };

  const saveMutation = useAdminMutation(onAuthError, {
    mutationFn: () => saveOfficialReviewReply(adminKey, review.id, content.trim()),
    onSuccess: async (updated) => {
      await invalidate(updated);
      setEditing(false);
      toast.show(review.officialReply ? "공식 답글을 수정했습니다." : "공식 답글을 등록했습니다.");
    },
  });
  const deleteMutation = useAdminMutation(onAuthError, {
    mutationFn: () => removeOfficialReviewReply(adminKey, review.id),
    onSuccess: async (updated) => {
      await invalidate(updated);
      setContent("");
      setConfirmingDelete(false);
      toast.show("공식 답글을 삭제했습니다.");
    },
  });

  return (
    <section className="admin-review-reply mt-3" aria-label="공방 공식 답글 관리">
      {!editing && review.officialReply && <ReviewOfficialReply reply={review.officialReply} />}
      {!editing && !confirmingDelete && (
        <div className="d-flex flex-wrap gap-2 mt-2">
          <Button
            type="button"
            size="sm"
            variant="outline-dark"
            onClick={() => {
              saveMutation.reset();
              deleteMutation.reset();
              setConfirmingDelete(false);
              setContent(review.officialReply?.content ?? "");
              setEditing(true);
            }}
          >
            {review.officialReply ? "답글 수정" : "공식 답글 작성"}
          </Button>
          {review.officialReply && (
            <Button
              type="button"
              size="sm"
              variant="outline-danger"
              onClick={() => {
                saveMutation.reset();
                deleteMutation.reset();
                setEditing(false);
                setConfirmingDelete(true);
              }}
            >
              답글 삭제
            </Button>
          )}
        </div>
      )}
      {editing && (
        <Form
          className="mt-2"
          onSubmit={(event) => {
            event.preventDefault();
            if (content.trim()) saveMutation.mutate();
          }}
        >
          <Form.Group controlId={`admin-review-reply-${review.id}`}>
            <Form.Label>공식 답글</Form.Label>
            <Form.Control
              as="textarea"
              rows={4}
              required
              maxLength={16000}
              value={content}
              disabled={saveMutation.isPending || deleteMutation.isPending}
              onChange={(event) => setContent(event.target.value)}
            />
          </Form.Group>
          <div className="d-flex gap-2 mt-2">
            <Button
              type="submit"
              size="sm"
              variant="dark"
              disabled={saveMutation.isPending || deleteMutation.isPending || !content.trim()}
            >
              {saveMutation.isPending ? "저장 중..." : "답글 저장"}
            </Button>
            <Button
              type="button"
              size="sm"
              variant="outline-secondary"
              disabled={saveMutation.isPending || deleteMutation.isPending}
              onClick={() => {
                saveMutation.reset();
                setEditing(false);
              }}
            >
              취소
            </Button>
          </div>
        </Form>
      )}
      {confirmingDelete && (
        <div className="admin-review-reply-delete mt-2" role="alert">
          <span>공식 답글을 삭제할까요?</span>
          <Button
            type="button"
            size="sm"
            variant="danger"
            disabled={deleteMutation.isPending || saveMutation.isPending}
            onClick={() => deleteMutation.mutate()}
          >
            {deleteMutation.isPending ? "삭제 중..." : "삭제"}
          </Button>
          <Button
            type="button"
            size="sm"
            variant="outline-secondary"
            disabled={deleteMutation.isPending || saveMutation.isPending}
            onClick={() => {
              deleteMutation.reset();
              setConfirmingDelete(false);
            }}
          >
            취소
          </Button>
        </div>
      )}
      <div className="mt-2"><ErrorAlert error={saveMutation.error ?? deleteMutation.error} /></div>
    </section>
  );
}
