import { Badge, Card } from "react-bootstrap";
import { Link } from "react-router-dom";
import type { InquiryResponse } from "@/shared/types";
import { EmptyState } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

interface Props {
  inquiries: InquiryResponse[] | undefined;
}

export function MyInquiriesSection({ inquiries }: Props) {
  const totalCount = inquiries?.length ?? 0;

  return (
    <section id="my-inquiries">
      <div className="d-flex justify-content-between align-items-center mb-2">
        <div>
          <h6 className="mb-1">내 문의</h6>
          <p className="text-muted-soft small mb-0">1:1 문의 내역을 확인합니다.</p>
        </div>
        <div className="d-flex align-items-center gap-3">
          <span className="text-muted-soft small">총 {totalCount}건</span>
          <Link to="/my/inquiries" className="my-inline-link small">전체 보기</Link>
        </div>
      </div>
      {inquiries && inquiries.length === 0 && <EmptyState message="등록된 문의가 없습니다." />}
      {inquiries && inquiries.length > 0 && inquiries.slice(0, 3).map((inq) => (
        <Card key={inq.id} className="mb-2 my-list-card border-0">
          <Card.Body className="py-2 px-3">
            <div className="d-flex justify-content-between align-items-center">
              <div className="d-flex align-items-center gap-2">
                <Badge bg={inq.hasReply ? "info" : "secondary"} className="badge-sm">
                  {inq.hasReply ? "답변완료" : "답변대기"}
                </Badge>
                <span className="fw-semibold small">{inq.title}</span>
              </div>
              <small className="text-muted-soft">{formatDateTime(inq.createdAt)}</small>
            </div>
          </Card.Body>
        </Card>
      ))}
    </section>
  );
}
