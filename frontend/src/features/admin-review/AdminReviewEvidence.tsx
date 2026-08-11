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
  unavailableMessage = "당시 후기 증거를 복구할 수 없는 이전 운영 이력입니다.",
}: Props) {
  if (!evidence) {
    return <Alert variant="secondary" className="small mt-2 mb-0">{unavailableMessage}</Alert>;
  }

  return (
    <div className="admin-review-evidence" aria-label="당시 후기 증거">
      <div className="d-flex flex-wrap align-items-center gap-2">
        <ReviewStars rating={evidence.rating} />
        <Badge bg="light" text="dark">본문 revision {evidence.contentRevision}</Badge>
        {evidence.provenance === "LEGACY_REPORT" && (
          <Badge bg="secondary">이전 신고 이관</Badge>
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
          이전 데이터에서 이관되어 당시 사진 증거는 완전하지 않을 수 있습니다.
        </Alert>
      )}
    </div>
  );
}
