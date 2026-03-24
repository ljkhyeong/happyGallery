import { useCallback } from "react";
import { Container, Card, Button } from "react-bootstrap";
import { useAdminKey } from "@/features/admin-product/useAdminKey";
import { AdminKeyGate } from "@/features/admin-product/AdminKeyGate";
import { ProductListSection } from "@/features/admin-product/ProductListSection";
import { CreateProductForm } from "@/features/admin-product/CreateProductForm";
import { CreateSlotForm } from "@/features/admin-slot/CreateSlotForm";
import { SlotListSection } from "@/features/admin-slot/SlotListSection";
import { BookingListSection } from "@/features/admin-booking/BookingListSection";
import { OrderListSection } from "@/features/admin-order/OrderListSection";
import { FailedRefundSection } from "@/features/admin-refund/FailedRefundSection";
import { PassActionPanel } from "@/features/admin-pass/PassActionPanel";
import { AdminQnaSection } from "@/features/admin-qna/AdminQnaSection";
import { AdminInquirySection } from "@/features/admin-inquiry/AdminInquirySection";
import { AdminNoticeSection } from "@/features/admin-notice/AdminNoticeSection";
import { useToast } from "@/shared/ui";

export function AdminPage() {
  const { adminKey, clearAdminKey, login, isAuthenticated } = useAdminKey();
  const toast = useToast();

  const handleAuthError = useCallback(() => {
    clearAdminKey();
    toast.show("인증이 만료되었습니다. 다시 로그인해 주세요.", "warning");
  }, [clearAdminKey, toast]);

  if (!isAuthenticated) {
    return <AdminKeyGate onLogin={login} />;
  }

  return (
    <Container className="page-container">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="mb-0">관리자</h4>
        <Button size="sm" variant="outline-secondary" onClick={clearAdminKey}>
          로그아웃
        </Button>
      </div>

      <Card className="mb-4">
        <Card.Header>공지사항 관리</Card.Header>
        <Card.Body>
          <AdminNoticeSection adminKey={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>상품 등록</Card.Header>
        <Card.Body>
          <CreateProductForm adminKey={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>상품 목록</Card.Header>
        <Card.Body>
          <ProductListSection adminKey={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>슬롯 생성</Card.Header>
        <Card.Body>
          <CreateSlotForm adminKey={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>슬롯 목록</Card.Header>
        <Card.Body>
          <SlotListSection adminKey={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>예약 목록</Card.Header>
        <Card.Body>
          <BookingListSection adminKey={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>주문 목록</Card.Header>
        <Card.Body>
          <OrderListSection adminKey={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>

      <div className="mb-4">
        <PassActionPanel adminKey={adminKey} onAuthError={handleAuthError} />
      </div>

      <Card className="mb-4">
        <Card.Header>환불 실패 목록</Card.Header>
        <Card.Body>
          <FailedRefundSection adminKey={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>Q&A 관리</Card.Header>
        <Card.Body>
          <AdminQnaSection token={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>1:1 문의 관리</Card.Header>
        <Card.Body>
          <AdminInquirySection token={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>
    </Container>
  );
}
