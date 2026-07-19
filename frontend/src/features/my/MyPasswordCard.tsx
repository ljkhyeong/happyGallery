import { Button, Card } from "react-bootstrap";
import { Link } from "react-router-dom";
import type { CustomerUser } from "@/features/customer-auth/useCustomerAuth";

interface Props {
  user: CustomerUser;
  onChangePassword: () => void;
  onRegisterPhone: () => void;
}

export function MyPasswordCard({ user, onChangePassword, onRegisterPhone }: Props) {
  return (
    <Card className="mb-4 my-action-card border-0">
      <Card.Body className="d-flex justify-content-between align-items-start gap-3">
        <div>
          <div className="my-section-kicker mb-2">계정 보안</div>
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
        ) : (
          <Button variant="primary" size="sm" onClick={onRegisterPhone}>
            휴대폰 등록
          </Button>
        )}
      </Card.Body>
    </Card>
  );
}
