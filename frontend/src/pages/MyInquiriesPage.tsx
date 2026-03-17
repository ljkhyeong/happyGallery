import { Container, Card, Badge, Button } from "react-bootstrap";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchMyInquiries } from "@/features/my-inquiry/api";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

export function MyInquiriesPage() {
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();

  const { data: inquiries, isLoading, error } = useQuery({
    queryKey: ["my", "inquiries"],
    queryFn: fetchMyInquiries,
    enabled: isAuthenticated,
  });

  if (authLoading || isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (!isAuthenticated) {
    return (
      <Container className="page-container">
        <Card className="text-center p-4">
          <p>로그인이 필요합니다.</p>
          <Link to="/login?redirectTo=/my/inquiries">로그인</Link>
        </Card>
      </Container>
    );
  }

  return (
    <Container className="page-container">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4>내 문의</h4>
        <Button as={Link as any} to="/my/inquiries/new" variant="primary" size="sm">
          문의 작성
        </Button>
      </div>

      <ErrorAlert error={error} />

      {(!inquiries || inquiries.length === 0) && (
        <EmptyState message="등록된 문의가 없습니다." />
      )}

      {inquiries?.map((inquiry) => (
        <Card key={inquiry.id} className="mb-2">
          <Card.Body className="py-2 px-3">
            <div className="d-flex justify-content-between align-items-start">
              <div>
                <div className="d-flex align-items-center gap-2 mb-1">
                  <Badge bg={inquiry.hasReply ? "info" : "secondary"} className="badge-sm">
                    {inquiry.hasReply ? "답변완료" : "답변대기"}
                  </Badge>
                  <span className="fw-semibold small">{inquiry.title}</span>
                </div>
                <div className="text-muted-soft" style={{ fontSize: "0.8rem" }}>
                  {formatDateTime(inquiry.createdAt)}
                </div>
              </div>
            </div>
            <div className="mt-2 small">
              <div className="bg-light p-2 rounded">{inquiry.content}</div>
              {inquiry.replyContent && (
                <div className="mt-2 p-2 rounded" style={{ background: "#f0f4ff" }}>
                  <strong className="small">답변</strong>
                  <div>{inquiry.replyContent}</div>
                </div>
              )}
            </div>
          </Card.Body>
        </Card>
      ))}

      <div className="mt-3">
        <Link to="/my" className="text-decoration-none">&larr; 마이페이지</Link>
      </div>
    </Container>
  );
}
