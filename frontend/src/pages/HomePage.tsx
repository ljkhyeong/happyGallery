import { Container, Row, Col } from "react-bootstrap";
import { Link } from "react-router-dom";

const SECTIONS = [
  {
    title: "상품 보기",
    desc: "핸드메이드 작품을 둘러보세요",
    path: "/products",
  },
  {
    title: "체험 예약",
    desc: "원하는 클래스와 시간을 골라 예약하세요",
    path: "/bookings/new",
  },
  {
    title: "예약 조회",
    desc: "예약 확인, 변경, 취소를 한 곳에서",
    path: "/bookings/manage",
  },
  {
    title: "8회권 구매",
    desc: "8회 이용권으로 더 합리적으로",
    path: "/passes/purchase",
  },
  {
    title: "주문하기",
    desc: "마음에 드는 작품을 주문하세요",
    path: "/orders/new",
  },
  {
    title: "주문 조회",
    desc: "주문 상태와 진행 현황을 확인하세요",
    path: "/orders/detail",
  },
] as const;

export function HomePage() {
  return (
    <>
      <section className="home-hero">
        <Container>
          <h1 className="display-6 mb-3">HappyGallery</h1>
          <p className="lead">
            손으로 만드는 즐거움을 나누는 핸드메이드 공방입니다.
            <br />
            체험 클래스 예약부터 작품 구매까지 한 곳에서 만나보세요.
          </p>
        </Container>
      </section>

      <Container className="page-container">
        <Row xs={1} sm={2} md={3} className="g-3">
          {SECTIONS.map(({ title, desc, path }) => (
            <Col key={path}>
              <Link to={path} className="home-card h-100">
                <div className="home-card-title">{title}</div>
                <p className="home-card-desc">{desc}</p>
              </Link>
            </Col>
          ))}
        </Row>
      </Container>
    </>
  );
}
