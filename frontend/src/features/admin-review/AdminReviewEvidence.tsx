import { Alert, Badge } from "react-bootstrap";
import type { ReviewEvidenceResponse } from "@/generated/api/review";
import { ReviewStars } from "@/features/review/ReviewDisplay";
import { formatDateTime } from "@/shared/lib";
import { AdminReviewProtectedImage } from "./AdminReviewProtectedImage";

interface Props {
  evidence: ReviewEvidenceResponse | null;
  adminKey: string;
  onAuthError: () => void;
  unavailableMessage?: string;
}

export function AdminReviewEvidence({
  evidence,
  adminKey,
  onAuthError,
  unavailableMessage = "이전 기록이라 당시 후기 내용을 확인할 수 없습니다.",
}: Props) {
  if (!evidence) {
    return <Alert variant="secondary" className="small mt-2 mb-0">{unavailableMessage}</Alert>;
  }

  return (
    <div className="admin-review-evidence" aria-label="당시 후기 증거">
      <div className="d-flex flex-wrap align-items-center gap-2">
        <ReviewStars rating={evidence.rating} />
        <Badge bg="light" text="dark">본문 버전 {evidence.contentRevision}</Badge>
        {evidence.provenance === "LEGACY_REPORT" && (
          <Badge bg="secondary">이전 신고 기록</Badge>
        )}
      </div>
      <p className="admin-review-content mt-2 mb-0">{evidence.content}</p>
      {evidence.imageUrls.length > 0 && (
        <div className="review-image-gallery" aria-label="당시 후기 사진 증거">
          {evidence.imageUrls.map((imageUrl, index) => (
            <AdminReviewProtectedImage
              key={`${imageUrl}-${index}`}
              adminKey={adminKey}
              source={{ kind: "evidence", evidenceId: evidence.id, sortOrder: index }}
              alt={`당시 후기 사진 ${index + 1}`}
              onAuthError={onAuthError}
            />
          ))}
        </div>
      )}
      <div className="small text-muted-soft mt-2">
        증거 저장 {formatDateTime(evidence.capturedAt)}
        {evidence.editedAt && ` · 본문 수정 ${formatDateTime(evidence.editedAt)}`}
      </div>
      {!evidence.imagesComplete && (
        <Alert variant="warning" className="small mt-2 mb-0">
          과거 신고 기록이라 당시 사진이 일부 남아 있지 않을 수 있습니다.
        </Alert>
      )}
    </div>
  );
}
