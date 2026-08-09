import { useQuery } from "@tanstack/react-query";
import { Badge, Card, Col, Container, Row } from "react-bootstrap";
import { Link, useParams } from "react-router";
import { getPublicClass } from "@/generated/api/booking";
import { PublicReviewSection } from "@/features/review/PublicReviewSection";
import { queryKeys } from "@/shared/api";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { formatKRW, isPositiveSafeIntegerString } from "@/shared/lib";
import { ErrorAlert, LinkButton, LoadingSpinner } from "@/shared/ui";
import { NotFoundPage } from "@/pages/NotFoundPage";

export function ClassDetailPage() {
  const { id } = useParams<{ id: string }>();
  const classId = Number(id);
  const validClassId = isPositiveSafeIntegerString(id);
  const classQuery = useQuery({
    queryKey: queryKeys.catalog.classDetail(classId),
    queryFn: ({ signal }) => getPublicClass(classId, { signal }),
    enabled: validClassId,
    staleTime: REFERENCE_DATA_STALE_TIME,
  });

  if (!validClassId) return <NotFoundPage />;
  if (classQuery.isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }
  if (classQuery.error) {
    return <Container className="page-container"><ErrorAlert error={classQuery.error} /></Container>;
  }
  if (!classQuery.data) return null;

  const bookingClass = classQuery.data;
  return (
    <Container className="page-container class-detail-page">
      <nav className="store-detail-breadcrumb" aria-label="경로">
        <Link to="/classes" className="store-detail-breadcrumb-link">클래스</Link>
        <span>/</span>
        <span className="store-detail-breadcrumb-current">{bookingClass.name}</span>
      </nav>

      <Row className="g-4 align-items-start">
        {bookingClass.imageUrl && (
          <Col lg={7}>
            <figure className="class-detail-media mb-0">
              <img src={bookingClass.imageUrl} alt={`${bookingClass.name} 수업`} />
            </figure>
          </Col>
        )}
        <Col lg={bookingClass.imageUrl ? 5 : 8}>
          <Card className="class-detail-card border-0">
            <Card.Body>
              <div className="d-flex align-items-center gap-2 mb-3">
                <Badge bg="dark">
                  {bookingClass.category === "PERFUME" ? "향수 클래스" : "공예 클래스"}
                </Badge>
                {bookingClass.passEligible && bookingClass.category !== "PERFUME" && (
                  <Badge bg="light" text="dark">8회권 사용 가능</Badge>
                )}
              </div>
              <p className="store-section-kicker mb-2">HappyGallery Class</p>
              <h1 className="class-detail-title">{bookingClass.name}</h1>
              <p className="text-muted-soft class-detail-description">
                {bookingClass.description || "재료와 과정을 차근차근 안내하는 해피갤러리 공예 수업입니다."}
              </p>
              <dl className="class-detail-meta">
                <div><dt>수강료</dt><dd>{formatKRW(bookingClass.price)}</dd></div>
                <div><dt>소요 시간</dt><dd>{bookingClass.durationMin}분</dd></div>
                {bookingClass.targetAudience && <div><dt>추천 대상</dt><dd>{bookingClass.targetAudience}</dd></div>}
                {bookingClass.preparationInfo && <div><dt>준비물</dt><dd>{bookingClass.preparationInfo}</dd></div>}
              </dl>
              <LinkButton
                to={`/bookings/new?classId=${bookingClass.id}`}
                variant="dark"
                size="lg"
                className="w-100"
              >
                날짜 선택하고 예약하기
              </LinkButton>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <PublicReviewSection targetType="CLASS" targetId={bookingClass.id} />
    </Container>
  );
}
