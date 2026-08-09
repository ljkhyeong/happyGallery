import { useCallback, useRef, useState, type ReactNode } from "react";
import { Alert, Button, Container, Form, Nav } from "react-bootstrap";
import { useSearchParams } from "react-router";
import { useAdminKey } from "@/features/admin-product/useAdminKey";
import { AdminLoginGate } from "@/features/admin-auth/AdminLoginGate";
import { AdminMfaSettings } from "@/features/admin-auth/AdminMfaSettings";
import { ProductListSection } from "@/features/admin-product/ProductListSection";
import { CreateProductForm } from "@/features/admin-product/CreateProductForm";
import { CreateClassForm } from "@/features/admin-class/CreateClassForm";
import { ClassListSection } from "@/features/admin-class/ClassListSection";
import { CreateSlotForm } from "@/features/admin-slot/CreateSlotForm";
import { BulkSlotForm } from "@/features/admin-slot/BulkSlotForm";
import { SlotListSection } from "@/features/admin-slot/SlotListSection";
import { BookingListSection } from "@/features/admin-booking/BookingListSection";
import { BookingCancellationTaskSection } from "@/features/admin-booking/BookingCancellationTaskSection";
import { OrderListSection } from "@/features/admin-order/OrderListSection";
import { AdminOrderClaimSection } from "@/features/admin-order-claim/AdminOrderClaimSection";
import { FailedRefundSection } from "@/features/admin-refund/FailedRefundSection";
import { FailedNotificationSection } from "@/features/admin-notification/FailedNotificationSection";
import { PaymentReconciliationSection } from "@/features/admin-payment-reconciliation/PaymentReconciliationSection";
import { PassActionPanel } from "@/features/admin-pass/PassActionPanel";
import { AdminQnaSection } from "@/features/admin-qna/AdminQnaSection";
import { AdminInquirySection } from "@/features/admin-inquiry/AdminInquirySection";
import { AdminNoticeSection } from "@/features/admin-notice/AdminNoticeSection";
import { AdminEventSection } from "@/features/admin-event/AdminEventSection";
import { AdminCouponSection } from "@/features/admin-coupon/AdminCouponSection";
import { AdminPasswordChangeForm } from "@/features/admin-auth/AdminPasswordChangeForm";
import { getAdminMfaStatus } from "@/features/admin-auth/api";
import { AdminDashboardSection } from "@/features/admin-dashboard/AdminDashboardSection";
import { AdminSearchSection } from "@/features/admin-search/AdminSearchSection";
import { WorkshopProfileForm } from "@/features/admin-workshop/WorkshopProfileForm";
import { AdminReviewSection } from "@/features/admin-review/AdminReviewSection";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import type { BookingStatus, OrderStatus } from "@/shared/types";

const ADMIN_VIEWS = [
  {
    value: "today",
    label: "오늘 할 일",
    description: "금전·전달 장애와 오늘 처리할 주문·예약을 우선 확인합니다.",
  },
  {
    value: "overview",
    label: "현황·검색",
    description: "매출과 운영 지표를 보고 주문·예약을 검색합니다.",
  },
  { value: "orders", label: "주문", description: "주문 승인부터 배송·픽업과 클레임까지 처리합니다." },
  {
    value: "bookings",
    label: "예약·8회권",
    description: "날짜별 예약과 8회권 정산을 관리합니다.",
  },
  { value: "products", label: "상품", description: "상품 정보와 판매 상태, 재고를 관리합니다." },
  {
    value: "classes",
    label: "클래스·슬롯",
    description: "클래스와 예약 가능한 일정을 관리합니다.",
  },
  {
    value: "events",
    label: "이벤트",
    description: "진행·예정 이벤트의 공개 일정과 홈 추천, 연관 상품을 관리합니다.",
  },
  {
    value: "coupons",
    label: "쿠폰",
    description: "회원 상품 주문 쿠폰의 할인 조건, 공개 발급 여부와 사용 기간을 관리합니다.",
  },
  {
    value: "support",
    label: "고객 응대",
    description: "공지사항과 상품 Q&A, 1:1 문의를 처리합니다.",
  },
  {
    value: "reviews",
    label: "후기",
    description: "상품·클래스 후기를 조회하고 공개 상태를 관리합니다.",
  },
  {
    value: "settings",
    label: "설정",
    description: "공개 공방 정보와 관리자 계정 보안을 관리합니다.",
  },
] as const;

type AdminView = (typeof ADMIN_VIEWS)[number]["value"];
const REQUIRE_ADMIN_MFA_ENROLLMENT =
  import.meta.env.PROD
  || import.meta.env.VITE_REQUIRE_ADMIN_MFA_ENROLLMENT === "true";

const ORDER_STATUSES: readonly OrderStatus[] = [
  "PAID_APPROVAL_PENDING",
  "APPROVED_FULFILLMENT_PENDING",
  "REJECTED",
  "CUSTOMER_CANCELED",
  "AUTO_REFUND_TIMEOUT",
  "IN_PRODUCTION",
  "DELAY_CONSENT_PENDING",
  "DELAY_ACCEPTED",
  "DELAY_REJECTED_CANCELED",
  "SHIPPING_PREPARING",
  "SHIPPED",
  "DELIVERED",
  "PICKUP_READY",
  "PICKED_UP",
  "PICKUP_EXPIRED",
  "PICKUP_FORFEITED",
  "COMPLETED",
];

const BOOKING_STATUSES: readonly BookingStatus[] = ["BOOKED", "CANCELED", "NO_SHOW", "COMPLETED"];

function isAdminView(value: string | null): value is AdminView {
  return ADMIN_VIEWS.some((view) => view.value === value);
}

function parsePositiveId(value: string | null): number | undefined {
  if (!value) return undefined;
  const parsed = Number(value);
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : undefined;
}

function parseOrderStatus(value: string | null): OrderStatus | undefined {
  return ORDER_STATUSES.find((status) => status === value);
}

function parseBookingStatus(value: string | null): BookingStatus | undefined {
  return BOOKING_STATUSES.find((status) => status === value);
}

function AdminPanel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="admin-workspace-panel">
      <h5>{title}</h5>
      {children}
    </section>
  );
}

export function AdminPage() {
  const {
    adminKey,
    clearAdminKey,
    login,
    verifyMfa,
    logout,
    isAuthenticated,
  } = useAdminKey();
  const toast = useToast();
  const handledExpiredKey = useRef<string | null>(null);
  const [loggingOut, setLoggingOut] = useState(false);
  const [searchParams, setSearchParams] = useSearchParams();
  const requestedView = searchParams.get("view");
  const activeView: AdminView = isAdminView(requestedView) ? requestedView : "today";
  const activeViewInfo = ADMIN_VIEWS.find((view) => view.value === activeView)!;
  const focusedOrderId = parsePositiveId(searchParams.get("orderId"));
  const focusedOrderStatus = parseOrderStatus(searchParams.get("orderStatus"));
  const focusedBookingId = parsePositiveId(searchParams.get("bookingId"));
  const focusedBookingStatus = parseBookingStatus(searchParams.get("bookingStatus"));
  const focusedBookingDate = searchParams.get("bookingDate") ?? undefined;

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

  const handleAdminCredentialsChanged = useCallback((message: string) => {
    clearAdminKey();
    toast.show(message, "success");
  }, [clearAdminKey, toast]);

  const productionMfaStatus = useAdminQuery(handleAuthError, {
    queryKey: ["admin", "auth", "mfa", "enrollment-gate"],
    queryFn: () => getAdminMfaStatus(adminKey),
    enabled: REQUIRE_ADMIN_MFA_ENROLLMENT && isAuthenticated,
  });

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await logout();
    } catch {
      toast.show(
        "이 브라우저에서는 로그아웃했습니다. 서버 세션 폐기를 확인하지 못했으므로 다른 기기의 접속 상태를 확인해 주세요.",
        "warning",
      );
    } finally {
      setLoggingOut(false);
    }
  };

  function selectView(view: AdminView) {
    const next = new URLSearchParams(searchParams);
    next.set("view", view);
    ["orderId", "orderStatus", "bookingId", "bookingDate", "bookingStatus"]
      .forEach((name) => next.delete(name));
    setSearchParams(next);
  }

  if (!isAuthenticated) {
    return <AdminLoginGate onLogin={login} onVerifyMfa={verifyMfa} />;
  }

  if (REQUIRE_ADMIN_MFA_ENROLLMENT && productionMfaStatus.isLoading) {
    return <LoadingSpinner text="관리자 보안 상태 확인 중..." />;
  }

  if (REQUIRE_ADMIN_MFA_ENROLLMENT && productionMfaStatus.error) {
    return (
      <Container className="page-container admin-workspace">
        <ErrorAlert error={productionMfaStatus.error} />
      </Container>
    );
  }

  if (REQUIRE_ADMIN_MFA_ENROLLMENT && productionMfaStatus.data?.enabled === false) {
    return (
      <Container className="page-container admin-workspace">
        <header className="admin-workspace-header">
          <h4 className="mb-0">관리자 2단계 인증 등록</h4>
          <Button
            size="sm"
            variant="outline-secondary"
            onClick={handleLogout}
            disabled={loggingOut}
          >
            {loggingOut ? "로그아웃 중..." : "로그아웃"}
          </Button>
        </header>
        <AdminPanel title="2단계 인증">
          <AdminMfaSettings
            adminKey={adminKey}
            onAuthError={handleAuthError}
            onCredentialChanged={handleAdminCredentialsChanged}
          />
        </AdminPanel>
      </Container>
    );
  }

  return (
    <Container className="page-container admin-workspace">
      <header className="admin-workspace-header">
        <h4 className="mb-0">관리자</h4>
        <Button
          size="sm"
          variant="outline-secondary"
          onClick={handleLogout}
          disabled={loggingOut}
        >
          {loggingOut ? "로그아웃 중..." : "로그아웃"}
        </Button>
      </header>

      {productionMfaStatus.data?.recoveryResetAvailable && (
        <Alert
          variant="warning"
          className="d-flex flex-wrap align-items-center justify-content-between gap-3"
        >
          <span>
            복구 코드로 로그인했습니다. 인증 앱을 사용할 수 없다면 세션이 만료되기 전에
            2단계 인증을 초기화하고 다시 등록해 주세요.
          </span>
          <Button
            type="button"
            size="sm"
            variant="outline-dark"
            onClick={() => selectView("settings")}
          >
            복구 진행
          </Button>
        </Alert>
      )}

      <Form.Select
        className="admin-workspace-mobile-nav d-sm-none"
        aria-label="관리 메뉴"
        value={activeView}
        onChange={(event) => selectView(event.target.value as AdminView)}
      >
        {ADMIN_VIEWS.map((view) => (
          <option key={view.value} value={view.value}>{view.label}</option>
        ))}
      </Form.Select>

      <Nav
        variant="tabs"
        className="admin-workspace-nav d-none d-sm-flex"
        activeKey={activeView}
        onSelect={(view) => view && selectView(view as AdminView)}
      >
        {ADMIN_VIEWS.map((view) => (
          <Nav.Item key={view.value}>
            <Nav.Link eventKey={view.value}>{view.label}</Nav.Link>
          </Nav.Item>
        ))}
      </Nav>

      <div className="admin-workspace-title">
        <h5>{activeViewInfo.label}</h5>
        <p>{activeViewInfo.description}</p>
      </div>

      {activeView === "today" && (
        <>
          <AdminPanel title="결제 대사 필요">
            <PaymentReconciliationSection adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="환불 확인 필요">
            <FailedRefundSection adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="주문 클레임 접수">
            <AdminOrderClaimSection
              adminKey={adminKey}
              onAuthError={handleAuthError}
              initialStatus="REQUESTED"
            />
          </AdminPanel>
          <AdminPanel title="알림 재처리 필요">
            <FailedNotificationSection adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="예약 취소 후속 작업">
            <BookingCancellationTaskSection adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="승인 대기 주문">
            <OrderListSection
              adminKey={adminKey}
              onAuthError={handleAuthError}
              initialStatus="PAID_APPROVAL_PENDING"
            />
          </AdminPanel>
          <AdminPanel title="오늘 예약">
            <BookingListSection adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
        </>
      )}

      {activeView === "overview" && (
        <>
          <AdminDashboardSection adminKey={adminKey} onAuthError={handleAuthError} />
          <AdminPanel title="주문·예약 검색">
            <AdminSearchSection adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
        </>
      )}

      {activeView === "orders" && (
        <>
          <AdminPanel title="주문 클레임">
            <AdminOrderClaimSection adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="주문 목록">
            <OrderListSection
              adminKey={adminKey}
              onAuthError={handleAuthError}
              initialStatus={focusedOrderStatus}
              focusOrderId={focusedOrderId}
              focusOrderStatus={focusedOrderStatus}
            />
          </AdminPanel>
        </>
      )}

      {activeView === "bookings" && (
        <>
          <AdminPanel title="예약 취소 후속 작업">
            <BookingCancellationTaskSection adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="예약 목록">
            <BookingListSection
              adminKey={adminKey}
              onAuthError={handleAuthError}
              initialDate={focusedBookingDate}
              initialStatus={focusedBookingStatus}
              focusBookingId={focusedBookingId}
            />
          </AdminPanel>
          <AdminPanel title="8회권 관리">
            <PassActionPanel adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
        </>
      )}

      {activeView === "products" && (
        <>
          <AdminPanel title="상품 등록">
            <CreateProductForm adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="상품 목록">
            <ProductListSection adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
        </>
      )}

      {activeView === "classes" && (
        <>
          <AdminPanel title="클래스 생성">
            <CreateClassForm adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="클래스 목록">
            <ClassListSection adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="슬롯 생성">
            <CreateSlotForm adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="슬롯 일괄 생성">
            <BulkSlotForm adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="슬롯 목록">
            <SlotListSection adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
        </>
      )}

      {activeView === "support" && (
        <>
          <AdminPanel title="공지사항 관리">
            <AdminNoticeSection adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="Q&A 관리">
            <AdminQnaSection token={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="1:1 문의 관리">
            <AdminInquirySection token={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
        </>
      )}

      {activeView === "reviews" && (
        <AdminPanel title="후기 관리">
          <AdminReviewSection adminKey={adminKey} onAuthError={handleAuthError} />
        </AdminPanel>
      )}

      {activeView === "events" && (
        <AdminPanel title="이벤트 관리">
          <AdminEventSection adminKey={adminKey} onAuthError={handleAuthError} />
        </AdminPanel>
      )}

      {activeView === "coupons" && (
        <AdminPanel title="쿠폰 관리">
          <AdminCouponSection adminKey={adminKey} onAuthError={handleAuthError} />
        </AdminPanel>
      )}

      {activeView === "settings" && (
        <>
          <AdminPanel title="공방 공개 정보">
            <WorkshopProfileForm adminKey={adminKey} onAuthError={handleAuthError} />
          </AdminPanel>
          <AdminPanel title="관리자 비밀번호">
            <AdminPasswordChangeForm
              adminKey={adminKey}
              onAuthError={handleAuthError}
              onChanged={handlePasswordChanged}
            />
          </AdminPanel>
          <AdminPanel title="2단계 인증">
            <AdminMfaSettings
              adminKey={adminKey}
              onAuthError={handleAuthError}
              onCredentialChanged={handleAdminCredentialsChanged}
            />
          </AdminPanel>
        </>
      )}
    </Container>
  );
}
