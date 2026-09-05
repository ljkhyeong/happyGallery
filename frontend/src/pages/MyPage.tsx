import { useEffect, useState } from "react";
import { Container, Badge } from "react-bootstrap";
import { Navigate, useLocation, useNavigate, useSearchParams } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { GuestClaimModal } from "@/features/customer-claim/GuestClaimModal";
import { AccountWithdrawalModal } from "@/features/customer-auth/AccountWithdrawalModal";
import { MemberEmailRegistrationModal } from "@/features/customer-auth/MemberEmailRegistrationModal";
import { MemberPhoneUpdateModal } from "@/features/customer-auth/MemberPhoneUpdateModal";
import { PasswordChangeModal } from "@/features/customer-auth/PasswordChangeModal";
import { SOCIAL_PROVIDER_DETAILS, type SocialProvider } from "@/features/customer-auth/socialAuth";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  fetchRecentMyBookings,
  fetchRecentMyOrders,
  fetchRecentMyPasses,
} from "@/features/my/api";
import { MyManagementLinks } from "@/features/my/MyManagementLinks";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import { MyDashboardHero } from "@/features/my/MyDashboardHero";
import { MyStatsRow } from "@/features/my/MyStatsRow";
import { MyClaimCard } from "@/features/my/MyClaimCard";
import { MyAccountCard } from "@/features/my/MyAccountCard";
import { MyOrdersSection } from "@/features/my/MyOrdersSection";
import { MyBookingsSection } from "@/features/my/MyBookingsSection";
import { getPassFilterKey } from "@/features/my/listUtils";
import { CustomerSessionChangedError, queryKeys } from "@/shared/api";
import { parseApiDateTime } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

export function MyPage() {
  const { sessionVersion } = useCustomerAuth();
  const { hash } = useLocation();
  const movedSection: Record<string, string> = {
    "#my-favorites": "/my/favorites",
    "#my-default-shipping-address": "/my/shipping-address",
    "#my-restock-alerts": "/my/restock-alerts",
    "#my-vacancy-alerts": "/my/vacancy-alerts",
    "#my-group-inquiries": "/my/group-inquiries",
  };
  if (movedSection[hash]) return <Navigate to={movedSection[hash]} replace />;
  return <MyPageContent key={sessionVersion} />;
}

function MyPageContent() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [showClaimModal, setShowClaimModal] = useState(false);
  const [showPhoneRegistration, setShowPhoneRegistration] = useState(false);
  const [phoneStepUpCompleted, setPhoneStepUpCompleted] = useState(false);
  const [showEmailRegistration, setShowEmailRegistration] = useState(false);
  const [emailStepUpCompleted, setEmailStepUpCompleted] = useState(false);
  const [showPasswordChange, setShowPasswordChange] = useState(false);
  const [showWithdrawal, setShowWithdrawal] = useState(false);
  const [phoneOnboardingHandled, setPhoneOnboardingHandled] = useState(false);
  const [claimModalSource, setClaimModalSource] = useState<string | null>(null);
  const [showClaimEntryHint, setShowClaimEntryHint] = useState(false);
  const { user, isAuthenticated, isLoading: authLoading, logout, withdraw, refresh } = useCustomerAuth();
  const [loggingOut, setLoggingOut] = useState(false);
  const toast = useToast();

  const {
    data: orders,
    isLoading: ordersLoading,
    isFetching: ordersFetching,
    error: ordersError,
    refetch: refetchOrders,
  } = useQuery({
    queryKey: queryKeys.member.orders.all,
    queryFn: ({ signal }) => fetchRecentMyOrders(signal),
    enabled: isAuthenticated,
  });

  const {
    data: bookings,
    isLoading: bookingsLoading,
    isFetching: bookingsFetching,
    error: bookingsError,
    refetch: refetchBookings,
  } = useQuery({
    queryKey: queryKeys.member.bookings.all,
    queryFn: ({ signal }) => fetchRecentMyBookings(signal),
    enabled: isAuthenticated,
  });

  const {
    data: passes,
    isLoading: passesLoading,
    isFetching: passesFetching,
    error: passesError,
    refetch: refetchPasses,
  } = useQuery({
    queryKey: queryKeys.member.passes,
    queryFn: ({ signal }) => fetchRecentMyPasses(signal),
    enabled: isAuthenticated,
  });

  const orderCount = orders?.length;
  const bookingCount = bookings?.length;
  const activePasses = passes?.filter((pass) => getPassFilterKey(pass) === "ACTIVE");
  const activePassCount = activePasses?.length;
  const remainingCredits = activePasses?.reduce((sum, pass) => sum + pass.remainingCredits, 0);
  const nextBooking = bookings
    ?.filter((booking) => booking.status === "BOOKED" && parseApiDateTime(booking.startAt) >= Date.now())
    .sort((a, b) => parseApiDateTime(a.startAt) - parseApiDateTime(b.startAt))[0];
  const latestOrder = orders?.[0];

  const navigationState = location.state as {
    phoneOnboarding?: boolean;
    phoneChangeRequested?: boolean;
    emailRegistrationRequested?: boolean;
    accountWithdrawalRequested?: boolean;
    socialAccountLinked?: string;
  } | null;
  const phoneOnboardingRequested = Boolean(navigationState?.phoneOnboarding);
  const linkedSocialProvider = navigationState?.socialAccountLinked;
  const phoneChangeRequested = Boolean(navigationState?.phoneChangeRequested);
  const emailRegistrationRequested = Boolean(
    navigationState?.emailRegistrationRequested,
  );
  const accountWithdrawalRequested = Boolean(
    navigationState?.accountWithdrawalRequested,
  );

  useEffect(() => {
    if (!isAuthenticated || !linkedSocialProvider) {
      return;
    }
    const provider = linkedSocialProvider.toLowerCase() as SocialProvider;
    const providerLabel = SOCIAL_PROVIDER_DETAILS[provider]?.label ?? "소셜 계정";
    toast.show(`${providerLabel} 계정을 연결했습니다.`, "success");
    navigate(`${location.pathname}${location.search}`, { replace: true, state: null });
  }, [isAuthenticated, linkedSocialProvider, location.pathname, location.search, navigate, toast]);

  useEffect(() => {
    if (
      !isAuthenticated
      || user?.phone !== null
      || phoneOnboardingHandled
      || accountWithdrawalRequested
      || emailRegistrationRequested
      || showEmailRegistration
    ) {
      return;
    }
    setShowPhoneRegistration(true);
  }, [
    accountWithdrawalRequested,
    emailRegistrationRequested,
    isAuthenticated,
    phoneOnboardingHandled,
    phoneOnboardingRequested,
    showEmailRegistration,
    user?.phone,
  ]);

  useEffect(() => {
    if (!isAuthenticated || !emailRegistrationRequested || user?.email) {
      return;
    }
    setEmailStepUpCompleted(true);
    setShowEmailRegistration(true);
    navigate(`${location.pathname}${location.search}`, { replace: true, state: null });
  }, [
    emailRegistrationRequested,
    isAuthenticated,
    location.pathname,
    location.search,
    navigate,
    user?.email,
  ]);

  useEffect(() => {
    if (!isAuthenticated || !phoneChangeRequested) {
      return;
    }
    setPhoneStepUpCompleted(true);
    setShowPhoneRegistration(true);
    navigate(`${location.pathname}${location.search}`, { replace: true, state: null });
  }, [
    isAuthenticated,
    phoneChangeRequested,
    location.pathname,
    location.search,
    navigate,
  ]);

  useEffect(() => {
    if (!isAuthenticated || !accountWithdrawalRequested) {
      return;
    }
    setPhoneOnboardingHandled(true);
    setShowPhoneRegistration(false);
    setShowWithdrawal(true);
    navigate(`${location.pathname}${location.search}`, { replace: true, state: null });
  }, [
    accountWithdrawalRequested,
    isAuthenticated,
    location.pathname,
    location.search,
    navigate,
  ]);

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
        <Badge bg="light" text="dark" className="mb-3">내 정보</Badge>
        <MyAuthGateCard
          title="로그인하고 주문, 예약, 8회권을 한 곳에서 관리하세요"
          description="회원은 추가 휴대폰 인증 없이 내 주문과 예약, 8회권, 쿠폰·적립금을 바로 확인할 수 있습니다. 비회원 주문과 예약도 별도 조회 화면에서 확인할 수 있습니다."
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
    setPhoneStepUpCompleted(false);
    if (phoneOnboardingRequested) {
      navigate(`${location.pathname}${location.search}`, { replace: true, state: null });
    }
  };

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await logout();
      navigate("/");
    } catch (error) {
      if (error instanceof CustomerSessionChangedError) return;
      toast.show(
        "로그아웃 완료를 확인하지 못해 현재 로그인 상태를 유지합니다. 잠시 후 다시 시도해 주세요.",
        "danger",
      );
    } finally {
      setLoggingOut(false);
    }
  };

  return (
    <Container className="page-container" style={{ maxWidth: 720 }}>
      <MyDashboardHero
        user={user!}
        nextBooking={nextBooking}
        onLogout={handleLogout}
        loggingOut={loggingOut}
      />

      {orderCount !== undefined
        && bookingCount !== undefined
        && remainingCredits !== undefined
        && activePassCount !== undefined && (
          <MyStatsRow
            orderCount={orderCount}
            bookingCount={bookingCount}
            remainingCredits={remainingCredits}
            activePassCount={activePassCount}
            latestOrder={latestOrder}
            nextBooking={nextBooking}
          />
        )}

      {passesLoading && <LoadingSpinner text="8회권 현황을 확인하고 있습니다" />}
      <ErrorAlert error={passesError} onRetry={() => { void refetchPasses(); }} retrying={passesFetching} />
      <MyManagementLinks />

      <MyClaimCard
        user={user!}
        showClaimEntryHint={showClaimEntryHint}
        onDismissHint={() => setShowClaimEntryHint(false)}
        onOpenClaim={handleOpenClaim}
      />

      <MyAccountCard
        user={user!}
        onChangePassword={() => setShowPasswordChange(true)}
        onUpdatePhone={() => {
          setPhoneOnboardingHandled(false);
          setPhoneStepUpCompleted(false);
          setShowPhoneRegistration(true);
        }}
        onRegisterEmail={() => {
          setEmailStepUpCompleted(false);
          setShowEmailRegistration(true);
        }}
        onWithdraw={() => setShowWithdrawal(true)}
      />

      <MyOrdersSection
        orders={orders}
        previewSize={1}
        isLoading={ordersLoading}
        error={ordersError}
        isFetching={ordersFetching}
        onRetry={() => void refetchOrders()}
      />

      <MyBookingsSection
        bookings={bookings}
        previewSize={1}
        isLoading={bookingsLoading}
        error={bookingsError}
        isFetching={bookingsFetching}
        onRetry={() => void refetchBookings()}
      />

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

      <MemberPhoneUpdateModal
        show={showPhoneRegistration}
        currentPhone={user!.phone}
        localPasswordEnabled={user!.localPasswordEnabled}
        initiallyReauthenticated={phoneStepUpCompleted}
        onClose={closePhoneRegistration}
        onUpdated={async () => {
          await refresh();
        }}
      />

      <MemberEmailRegistrationModal
        show={showEmailRegistration}
        localPasswordEnabled={user!.localPasswordEnabled}
        initiallyReauthenticated={emailStepUpCompleted}
        onClose={() => {
          setShowEmailRegistration(false);
          setEmailStepUpCompleted(false);
        }}
      />

      <AccountWithdrawalModal
        show={showWithdrawal}
        localPasswordEnabled={user!.localPasswordEnabled}
        onClose={() => setShowWithdrawal(false)}
        onWithdraw={async () => {
          await withdraw();
          toast.show("회원 탈퇴가 완료되었습니다.");
          navigate("/", { replace: true });
        }}
      />

      <PasswordChangeModal
        show={showPasswordChange}
        onClose={() => setShowPasswordChange(false)}
        onChanged={async () => {
          const refreshedUser = await refresh();
          if (!refreshedUser) {
            navigate("/login", { replace: true });
          }
        }}
      />
    </Container>
  );
}
