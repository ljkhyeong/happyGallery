import { Badge } from "react-bootstrap";
import { Link } from "react-router";
import type { CustomerUser } from "@/features/customer-auth/useCustomerAuth";
import { formatDateTime } from "@/shared/lib";
import { LinkButton } from "@/shared/ui/LinkButton";
import type { MyBookingSummary } from "./api";

interface Props {
  user: CustomerUser;
  nextBooking: MyBookingSummary | undefined;
}

/** 인사·계정 상태와 다음 예약을 한 줄에 둔다. 메뉴·로그아웃은 MyShell이 맡는다. */
export function MyDashboardHero({ user, nextBooking }: Props) {
  return (
    <section className="my-dashboard-hero" aria-labelledby="my-dashboard-title">
      <div className="my-dashboard-hello">
        <p className="store-section-kicker">내 정보</p>
        <h1 id="my-dashboard-title">{user.name}님, 다시 오셨네요</h1>
        <div className="my-dashboard-meta">
          <Badge bg={user.phoneVerified ? "success" : "secondary"}>
            {user.phoneVerified
              ? "휴대폰 인증 완료"
              : user.phone
                ? "휴대폰 재확인 필요"
                : "휴대폰 등록 필요"}
          </Badge>
          <span>{user.email}</span>
          {user.phone && <span>{user.phone}</span>}
        </div>
        <div className="my-dashboard-actions">
          <LinkButton to="/bookings/new" variant="primary" size="sm">체험 예약</LinkButton>
          <LinkButton to="/passes/purchase" variant="outline-dark" size="sm">4회권 구매</LinkButton>
        </div>
      </div>

      {nextBooking ? (
        <Link to={`/my/bookings/${nextBooking.bookingId}`} className="my-next-booking">
          <span className="my-next-booking-label">다음 예약</span>
          <strong>{nextBooking.className}</strong>
          <span>{formatDateTime(nextBooking.startAt)} · {nextBooking.participantCount}명</span>
          <span className="my-next-booking-link">예약 상세 보기 →</span>
        </Link>
      ) : (
        <div className="my-next-booking is-empty">
          <span className="my-next-booking-label">다음 예약</span>
          <strong>예정된 수업이 없어요</strong>
          <Link to="/classes" className="my-next-booking-link">수업 둘러보기 →</Link>
        </div>
      )}
    </section>
  );
}
