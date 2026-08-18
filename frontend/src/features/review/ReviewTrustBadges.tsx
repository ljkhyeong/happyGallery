import { Badge } from "react-bootstrap";
import { BadgeCheck, Pencil } from "lucide-react";

interface Props {
  verifiedTransaction: boolean;
  edited: boolean;
}

export function ReviewTrustBadges({ verifiedTransaction, edited }: Props) {
  return (
    <span className="review-trust-badges" aria-label="후기 작성 정보">
      {verifiedTransaction && (
        <Badge bg="light" text="dark" className="review-trust-badge">
          <BadgeCheck size={14} aria-hidden="true" /> 실제 이용
        </Badge>
      )}
      {edited && (
        <Badge bg="light" text="secondary" className="review-trust-badge">
          <Pencil size={12} aria-hidden="true" /> 수정됨
        </Badge>
      )}
    </span>
  );
}
