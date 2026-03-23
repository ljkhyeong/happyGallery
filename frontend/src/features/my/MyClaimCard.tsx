import { Button, Card } from "react-bootstrap";
import type { CustomerUser } from "@/features/customer-auth/useCustomerAuth";

interface Props {
  user: CustomerUser;
  showClaimEntryHint: boolean;
  onDismissHint: () => void;
  onOpenClaim: (source: string) => void;
}

export function MyClaimCard({ user, showClaimEntryHint, onDismissHint, onOpenClaim }: Props) {
  return (
    <Card className="mb-4 my-claim-card border-0">
      <Card.Body>
        {showClaimEntryHint && (
          <div className="my-claim-entry-note mb-3">
            <div>
              <strong>회원가입이 완료되었습니다.</strong>
              <div className="small text-muted-soft">
                같은 번호의 비회원 이력이 있다면 지금 바로 가져올 수 있습니다.
              </div>
            </div>
            <div className="d-flex flex-wrap gap-2">
              <Button size="sm" variant="dark" onClick={() => onOpenClaim("claim_entry_hint")}>
                지금 확인
              </Button>
              <Button size="sm" variant="outline-secondary" onClick={onDismissHint}>
                닫기
              </Button>
            </div>
          </div>
        )}
        <div className="d-flex justify-content-between align-items-start gap-3">
          <div>
            <div className="my-section-kicker mb-2">Guest Claim</div>
            <h6 className="mb-1">비회원 이력 가져오기</h6>
            <p className="text-muted-soft small mb-0">
              {user.phoneVerified
                ? "같은 휴대폰 번호로 남긴 비회원 주문과 예약을 이 계정으로 이전할 수 있습니다."
                : "먼저 같은 번호인지 한 번 더 확인한 뒤 비회원 주문과 예약을 가져올 수 있습니다."}
            </p>
          </div>
          <Button
            variant={user.phoneVerified ? "outline-primary" : "primary"}
            size="sm"
            onClick={() => onOpenClaim("claim_dashboard_card")}
          >
            {user.phoneVerified ? "이력 가져오기" : "휴대폰 확인 후 가져오기"}
          </Button>
        </div>
      </Card.Body>
    </Card>
  );
}
