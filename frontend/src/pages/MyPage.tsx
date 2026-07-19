import { useEffect, useState } from "react";
import { Container, Badge } from "react-bootstrap";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { GuestClaimModal } from "@/features/customer-claim/GuestClaimModal";
import { InitialPhoneRegistrationModal } from "@/features/customer-auth/InitialPhoneRegistrationModal";
import { PasswordChangeModal } from "@/features/customer-auth/PasswordChangeModal";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { fetchMyBookings, fetchMyOrders, fetchMyPasses } from "@/features/my/api";
import { fetchMyInquiries } from "@/features/my-inquiry/api";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import { MyDashboardHero } from "@/features/my/MyDashboardHero";
import { MyStatsRow } from "@/features/my/MyStatsRow";
import { MyClaimCard } from "@/features/my/MyClaimCard";
import { MyPasswordCard } from "@/features/my/MyPasswordCard";
import { MyOrdersSection } from "@/features/my/MyOrdersSection";
import { MyBookingsSection } from "@/features/my/MyBookingsSection";
import { MyPassesSection } from "@/features/my/MyPassesSection";
import { MyInquiriesSection } from "@/features/my/MyInquiriesSection";
import { getPassFilterKey } from "@/features/my/listUtils";
import { LoadingSpinner } from "@/shared/ui";

export function MyPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [showClaimModal, setShowClaimModal] = useState(false);
  const [showPhoneRegistration, setShowPhoneRegistration] = useState(false);
  const [showPasswordChange, setShowPasswordChange] = useState(false);
  const [phoneOnboardingHandled, setPhoneOnboardingHandled] = useState(false);
  const [claimModalSource, setClaimModalSource] = useState<string | null>(null);
  const [showClaimEntryHint, setShowClaimEntryHint] = useState(false);
  const { user, isAuthenticated, isLoading: authLoading, logout, refresh } = useCustomerAuth();

  const { data: orders, isLoading: ordersLoading, error: ordersError } = useQuery({
    queryKey: ["my", "orders"],
    queryFn: fetchMyOrders,
    enabled: isAuthenticated,
  });

  const { data: bookings, isLoading: bookingsLoading, error: bookingsError } = useQuery({
    queryKey: ["my", "bookings"],
    queryFn: fetchMyBookings,
    enabled: isAuthenticated,
  });

  const { data: passes, isLoading: passesLoading, error: passesError } = useQuery({
    queryKey: ["my", "passes"],
    queryFn: fetchMyPasses,
    enabled: isAuthenticated,
  });

  const { data: inquiries } = useQuery({
    queryKey: ["my", "inquiries"],
    queryFn: fetchMyInquiries,
    enabled: isAuthenticated,
  });

  const orderCount = orders?.length ?? 0;
  const bookingCount = bookings?.length ?? 0;
  const passCount = passes?.length ?? 0;
  const activePasses = passes?.filter((pass) => getPassFilterKey(pass) === "ACTIVE") ?? [];
  const activePassCount = activePasses.length;
  const remainingCredits = activePasses.reduce((sum, pass) => sum + pass.remainingCredits, 0);
  const nextBooking = bookings
    ?.filter((booking) => booking.status === "BOOKED" && Date.parse(booking.startAt) >= Date.now())
    .sort((a, b) => Date.parse(a.startAt) - Date.parse(b.startAt))[0];
  const latestOrder = orders?.[0];

  const phoneOnboardingRequested = Boolean(
    (location.state as { phoneOnboarding?: boolean } | null)?.phoneOnboarding,
  );

  useEffect(() => {
    if (!isAuthenticated || user?.phone !== null || phoneOnboardingHandled) {
      return;
    }
    setShowPhoneRegistration(true);
  }, [isAuthenticated, phoneOnboardingHandled, phoneOnboardingRequested, user?.phone]);

  useEffect(() => {
    if (!isAuthenticated || searchParams.get("claim") !== "1") {
      return;
    }
    setShowClaimEntryHint(true);
    setClaimModalSource("claim_query_auto_open");
    setShowClaimModal(true);
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.delete("claim");
    setSearchParams(nextSearchParams, { replace: true });
  }, [isAuthenticated, searchParams, setSearchParams]);

  if (authLoading) return <Container className="page-container"><LoadingSpinner /></Container>;

  if (!isAuthenticated) {
    return (
      <Container className="page-container" style={{ maxWidth: 760 }}>
        <Badge bg="light" text="dark" className="mb-3">Member Self Service</Badge>
        <MyAuthGateCard
          title="로그인하고 주문, 예약, 8회권을 한 곳에서 관리하세요"
          description="회원은 추가 휴대폰 인증 없이 내 주문과 예약, 8회권을 바로 확인할 수 있습니다. 비회원 조회가 필요하면 guest 경로를 그대로 사용할 수 있습니다."
          showGuestLinks
        />
      </Container>
    );
  }

  const handleOpenClaim = (source: string) => {
    if (!user?.phone) {
      setPhoneOnboardingHandled(false);
      setShowPhoneRegistration(true);
      return;
    }
    setClaimModalSource(source);
    setShowClaimModal(true);
  };

  const closePhoneRegistration = () => {
    setPhoneOnboardingHandled(true);
    setShowPhoneRegistration(false);
    if (phoneOnboardingRequested) {
      navigate(`${location.pathname}${location.search}`, { replace: true, state: null });
    }
  };

  return (
    <Container className="page-container" style={{ maxWidth: 720 }}>
      <MyDashboardHero
        user={user!}
        nextBooking={nextBooking}
        onLogout={() => { logout(); navigate("/"); }}
      />

      <MyStatsRow
        orderCount={orderCount}
        bookingCount={bookingCount}
        remainingCredits={remainingCredits}
        activePassCount={activePassCount}
        latestOrder={latestOrder}
        nextBooking={nextBooking}
      />

      <MyClaimCard
        user={user!}
        showClaimEntryHint={showClaimEntryHint}
        onDismissHint={() => setShowClaimEntryHint(false)}
        onOpenClaim={handleOpenClaim}
      />

      <MyPasswordCard
        user={user!}
        onChangePassword={() => setShowPasswordChange(true)}
        onRegisterPhone={() => {
          setPhoneOnboardingHandled(false);
          setShowPhoneRegistration(true);
        }}
      />

      <MyOrdersSection
        orders={orders}
        isLoading={ordersLoading}
        error={ordersError}
        totalCount={orderCount}
      />

      <MyBookingsSection
        bookings={bookings}
        isLoading={bookingsLoading}
        error={bookingsError}
        totalCount={bookingCount}
      />

      <MyPassesSection
        passes={passes}
        isLoading={passesLoading}
        error={passesError}
        totalCount={passCount}
      />

      <MyInquiriesSection inquiries={inquiries} />

      {showClaimModal && user!.phone && (
        <GuestClaimModal
          show={showClaimModal}
          onClose={() => setShowClaimModal(false)}
          phone={user!.phone}
          phoneVerified={user!.phoneVerified}
          onPhoneVerified={async () => {
            await refresh();
          }}
          monitoringSource={claimModalSource}
        />
      )}

      <InitialPhoneRegistrationModal
        show={showPhoneRegistration}
        onClose={closePhoneRegistration}
        onRegistered={async () => {
          await refresh();
        }}
      />

      <PasswordChangeModal
        show={showPasswordChange}
        onClose={() => setShowPasswordChange(false)}
        onChanged={async () => {
          await refresh();
          navigate("/login", { replace: true });
        }}
      />
    </Container>
  );
}
