import { Button, Card } from "react-bootstrap";
import { Link } from "react-router-dom";
import { SocialAccountSection } from "@/features/customer-auth/SocialAccountSection";
import type { CustomerUser } from "@/features/customer-auth/useCustomerAuth";

interface Props {
  user: CustomerUser;
  onChangePassword: () => void;
  onUpdatePhone: () => void;
  onWithdraw: () => void;
}

export function MyAccountCard({ user, onChangePassword, onUpdatePhone, onWithdraw }: Props) {
  return (
    <Card className="mb-4 my-action-card border-0">
      <Card.Body>
        <div className="my-section-kicker mb-3">계정 관리</div>
        <div className="d-flex justify-content-between align-items-start gap-3">
          <div>
            <h6 className="mb-1">로그인 비밀번호</h6>
            <p className="text-muted-soft small mb-0">
              {user.localPasswordEnabled
                ? "변경하면 현재 로그인된 모든 기기에서 로그아웃됩니다."
                : "휴대폰 인증으로 이메일 로그인 비밀번호를 설정할 수 있습니다."}
            </p>
          </div>
          {user.localPasswordEnabled ? (
            <Button variant="outline-primary" size="sm" onClick={onChangePassword}>
              변경
            </Button>
          ) : user.phone ? (
            <Link
              className="btn btn-outline-primary btn-sm"
              to="/forgot-password"
              state={{ email: user.email, phone: user.phone }}
            >
              설정
            </Link>
          ) : null}
        </div>

        <div className="d-flex justify-content-between align-items-start gap-3 border-top mt-3 pt-3">
          <div>
            <h6 className="mb-1">휴대폰 번호</h6>
            <p className="text-muted-soft small mb-0">
              {user.phone ?? "등록된 휴대폰 번호가 없습니다."}
            </p>
          </div>
          <Button variant="outline-primary" size="sm" onClick={onUpdatePhone}>
            {user.phone ? "변경" : "등록"}
          </Button>
        </div>

        <SocialAccountSection localPasswordEnabled={user.localPasswordEnabled} />

        <div className="d-flex justify-content-between align-items-start gap-3 border-top mt-3 pt-3">
          <div>
            <h6 className="mb-1">회원 탈퇴</h6>
            <p className="text-muted-soft small mb-0">
              진행 중인 주문·예약·환불이나 사용 가능한 8회권이 있으면 탈퇴할 수 없습니다.
            </p>
          </div>
          <Button variant="outline-danger" size="sm" onClick={onWithdraw}>
            탈퇴
          </Button>
        </div>
      </Card.Body>
    </Card>
  );
}
