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
                같은 휴대폰 번호로 남긴 비회원 주문·예약을 가져올 수 있습니다.
              </div>
            </div>
            <div className="d-flex flex-wrap gap-2">
              <Button size="sm" variant="dark" onClick={() => onOpenClaim("claim_entry_hint")}>
                가져올 내역 확인
              </Button>
              <Button size="sm" variant="outline-secondary" onClick={onDismissHint}>
                닫기
              </Button>
            </div>
          </div>
        )}
        <div className="d-flex justify-content-between align-items-start gap-3">
          <div>
            <h6 className="mb-1">비회원 주문·예약 가져오기</h6>
            <p className="text-muted-soft small mb-0">
              {!user.phone
                ? "휴대폰 번호를 등록하면 같은 번호로 남긴 비회원 주문과 예약을 가져올 수 있습니다."
                : user.phoneVerified
                ? "같은 휴대폰 번호로 남긴 비회원 주문과 예약을 이 계정으로 가져올 수 있습니다."
                : "휴대폰 번호를 다시 인증하면 비회원 주문·예약을 가져올 수 있습니다."}
            </p>
          </div>
          <Button
            variant={user.phoneVerified && user.phone ? "outline-primary" : "primary"}
            size="sm"
            onClick={() => onOpenClaim("claim_dashboard_card")}
          >
            {!user.phone
              ? "휴대폰 등록"
              : user.phoneVerified
                ? "주문·예약 가져오기"
                : "휴대폰 인증 후 가져오기"}
          </Button>
        </div>
      </Card.Body>
    </Card>
  );
}
