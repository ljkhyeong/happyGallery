import { Col, Row } from "react-bootstrap";
import { Link } from "react-router";

const links = [
  { to: "/my/orders", title: "전체 주문", description: "주문 상태·배송·취소" },
  { to: "/my/bookings", title: "전체 예약", description: "일정 변경·예약 취소" },
  { to: "/my/passes", title: "내 8회권", description: "남은 횟수·만료·환불" },
  { to: "/my/favorites", title: "내 찜", description: "관심 상품과 클래스" },
  { to: "/my/shipping-address", title: "기본 배송지", description: "배송지 저장·수정" },
  { to: "/my/notifications", title: "전체 알림", description: "읽지 않은 알림과 이전 알림" },
  { to: "/my/restock-alerts", title: "재입고 알림 신청", description: "품절 상품 알림 관리" },
  { to: "/my/vacancy-alerts", title: "예약 빈자리 알림 신청", description: "마감된 회차 알림 관리" },
  { to: "/my/group-inquiries", title: "단체 수업 문의", description: "접수 내역과 상담 상태" },
  { to: "/my/inquiries", title: "내 문의", description: "상품·예약 관련 문의" },
];

export function MyManagementLinks() {
  return (
    <nav aria-label="내 정보 관리 메뉴" className="mb-4">
      <Row className="g-2">
        {links.map(({ to, title, description }) => (
          <Col xs={12} sm={6} key={to}>
            <Link to={to} className="d-block border rounded p-3 h-100 text-decoration-none text-body">
              <strong>{title} →</strong>
              <span className="d-block small text-muted mt-1">{description}</span>
            </Link>
          </Col>
        ))}
      </Row>
    </nav>
  );
}
