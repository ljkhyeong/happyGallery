import { useCallback, useRef, useState } from "react";
import { Container, Card, Button } from "react-bootstrap";
import { useAdminKey } from "@/features/admin-product/useAdminKey";
import { AdminKeyGate } from "@/features/admin-product/AdminKeyGate";
import { ProductListSection } from "@/features/admin-product/ProductListSection";
import { CreateProductForm } from "@/features/admin-product/CreateProductForm";
import { CreateClassForm } from "@/features/admin-class/CreateClassForm";
import { CreateSlotForm } from "@/features/admin-slot/CreateSlotForm";
import { SlotListSection } from "@/features/admin-slot/SlotListSection";
import { BookingListSection } from "@/features/admin-booking/BookingListSection";
import { OrderListSection } from "@/features/admin-order/OrderListSection";
import { FailedRefundSection } from "@/features/admin-refund/FailedRefundSection";
import { FailedNotificationSection } from "@/features/admin-notification/FailedNotificationSection";
import { PaymentReconciliationSection } from "@/features/admin-payment-reconciliation/PaymentReconciliationSection";
import { PassActionPanel } from "@/features/admin-pass/PassActionPanel";
import { AdminQnaSection } from "@/features/admin-qna/AdminQnaSection";
import { AdminInquirySection } from "@/features/admin-inquiry/AdminInquirySection";
import { AdminNoticeSection } from "@/features/admin-notice/AdminNoticeSection";
import { AdminPasswordChangeForm } from "@/features/admin-auth/AdminPasswordChangeForm";
import { AdminDashboardSection } from "@/features/admin-dashboard/AdminDashboardSection";
import { AdminSearchSection } from "@/features/admin-search/AdminSearchSection";
import { useToast } from "@/shared/ui";

export function AdminPage() {
  const { adminKey, clearAdminKey, login, logout, isAuthenticated } = useAdminKey();
  const toast = useToast();
  const handledExpiredKey = useRef<string | null>(null);
  const [loggingOut, setLoggingOut] = useState(false);

  const handleAuthError = useCallback(() => {
    if (handledExpiredKey.current === adminKey) return;
    handledExpiredKey.current = adminKey;
    clearAdminKey();
    toast.show("인증이 만료되었습니다. 다시 로그인해 주세요.", "warning");
  }, [adminKey, clearAdminKey, toast]);

  const handlePasswordChanged = useCallback(() => {
    clearAdminKey();
    toast.show("비밀번호가 변경되었습니다. 새 비밀번호로 다시 로그인해 주세요.", "success");
  }, [clearAdminKey, toast]);

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await logout();
    } catch {
      toast.show(
        "로그아웃 완료를 확인하지 못해 관리자 세션과 현재 로그인 상태를 유지합니다.",
        "danger",
      );
    } finally {
      setLoggingOut(false);
    }
  };

  if (!isAuthenticated) {
    return <AdminKeyGate onLogin={login} />;
  }

  return (
    <Container className="page-container">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h4 className="mb-0">관리자</h4>
        <Button
          size="sm"
          variant="outline-secondary"
          onClick={handleLogout}
          disabled={loggingOut}
        >
          {loggingOut ? "로그아웃 중..." : "로그아웃"}
        </Button>
      </div>

      <div className="mb-4">
        <AdminDashboardSection adminKey={adminKey} onAuthError={handleAuthError} />
      </div>

      <Card className="mb-4">
        <Card.Header>주문·예약 검색</Card.Header>
        <Card.Body>
          <AdminSearchSection adminKey={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>관리자 비밀번호</Card.Header>
        <Card.Body>
          <AdminPasswordChangeForm
            adminKey={adminKey}
            onAuthError={handleAuthError}
            onChanged={handlePasswordChanged}
          />
        </Card.Body>
      </Card>

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
        <Card.Header>클래스 생성</Card.Header>
        <Card.Body>
          <CreateClassForm adminKey={adminKey} onAuthError={handleAuthError} />
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
        <Card.Header>환불 확인 필요</Card.Header>
        <Card.Body>
          <FailedRefundSection adminKey={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>결제 대사 필요</Card.Header>
        <Card.Body>
          <PaymentReconciliationSection adminKey={adminKey} onAuthError={handleAuthError} />
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Header>알림 재처리 필요</Card.Header>
        <Card.Body>
          <FailedNotificationSection adminKey={adminKey} onAuthError={handleAuthError} />
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
