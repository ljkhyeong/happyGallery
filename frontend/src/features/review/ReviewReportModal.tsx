import { useId, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Flag } from "lucide-react";
import { Button, Form, Modal } from "react-bootstrap";
import { Link } from "react-router";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { submitReviewReport, type ReviewReportReason } from "./api";

const REPORT_REASON_LABELS: Record<ReviewReportReason, string> = {
  SPAM: "광고·도배",
  ABUSIVE: "욕설·비방",
  PRIVACY: "개인정보 노출",
  FALSE_INFORMATION: "허위 정보",
  OTHER: "기타",
};

interface Props {
  reviewId: number;
  reportedByMe?: boolean;
  canInteract?: boolean;
  reactionLoading: boolean;
  isAuthenticated: boolean;
  loginHref: string;
  interactionDescriptionId?: string;
}

export function ReviewReportModal({
  reviewId,
  reportedByMe,
  canInteract,
  reactionLoading,
  isAuthenticated,
  loginHref,
  interactionDescriptionId,
}: Props) {
  const titleId = useId();
  const [show, setShow] = useState(false);
  const [reason, setReason] = useState<ReviewReportReason>("SPAM");
  const [detail, setDetail] = useState("");
  const queryClient = useQueryClient();
  const toast = useToast();
  const mutation = useMutation({
    mutationFn: () => runForCurrentCustomer(
      () => submitReviewReport(reviewId, { reason, detail: detail.trim() || null }),
      async (_, requireCurrent) => {
        requireCurrent();
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: queryKeys.member.reviews.all }),
          queryClient.invalidateQueries({ queryKey: queryKeys.admin.reviews.reports.all }),
        ]);
        requireCurrent();
        setShow(false);
        setDetail("");
        toast.show("후기를 신고했습니다. 운영자가 내용을 확인합니다.");
      },
    ),
  });

  const closeModal = () => {
    if (mutation.isPending) return;
    setShow(false);
    setReason("SPAM");
    setDetail("");
    mutation.reset();
  };

  if (!isAuthenticated) {
    return (
      <Link to={loginHref} className="btn btn-sm btn-link text-muted review-report-link">
        <Flag size={14} aria-hidden="true" /> 로그인 후 신고
      </Link>
    );
  }

  return (
    <>
      <Button
        type="button"
        size="sm"
        variant="link"
        className="text-muted review-report-link"
        aria-describedby={interactionDescriptionId}
        disabled={
          reactionLoading
          || reportedByMe === undefined
          || canInteract === false
          || reportedByMe
        }
        onClick={() => setShow(true)}
      >
        <Flag size={14} aria-hidden="true" /> {reportedByMe ? "신고 접수됨" : "신고"}
      </Button>
      <Modal
        show={show}
        onHide={closeModal}
        centered
        aria-labelledby={titleId}
      >
        <Form
          onSubmit={(event) => {
            event.preventDefault();
            mutation.mutate();
          }}
        >
          <Modal.Header closeButton={!mutation.isPending}>
            <Modal.Title id={titleId} className="fs-6">후기 신고</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <p className="text-muted-soft small">
              운영 정책 위반이 의심되는 이유를 알려주세요. 신고만으로 후기가 자동 숨김 처리되지는 않습니다.
            </p>
            <Form.Group controlId={`review-report-reason-${reviewId}`} className="mb-3">
              <Form.Label>신고 사유</Form.Label>
              <Form.Select
                value={reason}
                disabled={mutation.isPending}
                onChange={(event) => setReason(event.target.value as ReviewReportReason)}
              >
                {Object.entries(REPORT_REASON_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </Form.Select>
            </Form.Group>
            <Form.Group controlId={`review-report-detail-${reviewId}`}>
              <Form.Label>상세 내용 <span className="text-muted-soft">(선택)</span></Form.Label>
              <Form.Control
                as="textarea"
                rows={4}
                maxLength={1000}
                value={detail}
                disabled={mutation.isPending}
                onChange={(event) => setDetail(event.target.value)}
              />
              <Form.Text>{detail.length.toLocaleString("ko-KR")} / 1,000자</Form.Text>
            </Form.Group>
            <div className="mt-3"><ErrorAlert error={mutation.error} /></div>
          </Modal.Body>
          <Modal.Footer>
            <Button
              type="button"
              variant="outline-secondary"
              disabled={mutation.isPending}
              onClick={closeModal}
            >
              취소
            </Button>
            <Button type="submit" variant="danger" disabled={mutation.isPending}>
              {mutation.isPending ? "접수 중..." : "신고 접수"}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>
    </>
  );
}
