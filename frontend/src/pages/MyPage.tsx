import { useEffect, useState } from "react";
import { useLocation, useNavigate, useSearchParams } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { GuestClaimModal } from "@/features/customer-claim/GuestClaimModal";
import { AccountWithdrawalModal } from "@/features/customer-auth/AccountWithdrawalModal";
import { MemberEmailRegistrationModal } from "@/features/customer-auth/MemberEmailRegistrationModal";
import { MemberPhoneUpdateModal } from "@/features/customer-auth/MemberPhoneUpdateModal";
import { resolveSafeReturnTo } from "@/features/customer-auth/navigation";
import { PasswordChangeModal } from "@/features/customer-auth/PasswordChangeModal";
import { SOCIAL_PROVIDER_DETAILS, type SocialProvider } from "@/features/customer-auth/socialAuth";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  fetchRecentMyBookings,
  fetchRecentMyOrders,
  fetchRecentMyPasses,
} from "@/features/my/api";
import { MyDashboardHero } from "@/features/my/MyDashboardHero";
import { MyStatsRow } from "@/features/my/MyStatsRow";
import { MyClaimCard } from "@/features/my/MyClaimCard";
import { MyAccountCard } from "@/features/my/MyAccountCard";
import { MyOrdersSection } from "@/features/my/MyOrdersSection";
import { MyBookingsSection } from "@/features/my/MyBookingsSection";
import { getPassFilterKey } from "@/features/my/listUtils";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { parseApiDateTime } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

export function MyPage() {
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
  // 다른 화면에서 휴대폰 인증을 요청하며 들어오면 등록을 마친 뒤 그 화면으로 돌아간다.
  const [phoneReturnTo, setPhoneReturnTo] = useState<string | null>(null);
  const [showClaimEntryHint, setShowClaimEntryHint] = useState(false);
  const { user, isAuthenticated, withdraw, refresh } = useCustomerAuth();
  const toast = useToast();

  const {
    data: orders,
    isLoading: ordersLoading,
    isFetching: ordersFetching,
    error: ordersError,
    refetch: refetchOrders,
  } = useQuery({
    queryKey: queryKeys.member.orders.all,
    queryFn: ({ signal }) => runForCurrentCustomer(() => fetchRecentMyOrders(signal)),
  });

  const {
    data: bookings,
    isLoading: bookingsLoading,
    isFetching: bookingsFetching,
    error: bookingsError,
    refetch: refetchBookings,
  } = useQuery({
    queryKey: queryKeys.member.bookings.all,
    queryFn: ({ signal }) => runForCurrentCustomer(() => fetchRecentMyBookings(signal)),
  });

  const {
    data: passes,
    isLoading: passesLoading,
    isFetching: passesFetching,
    error: passesError,
    refetch: refetchPasses,
  } = useQuery({
    queryKey: queryKeys.member.passes,
    queryFn: ({ signal }) => runForCurrentCustomer(() => fetchRecentMyPasses(signal)),
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
    phoneChangeRequested?: boolean;
    emailRegistrationRequested?: boolean;
    accountWithdrawalRequested?: boolean;
    socialAccountLinked?: string;
  } | null;
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
    setShowClaimModal(true);
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.delete("claim");
    setSearchParams(nextSearchParams, { replace: true });
  }, [isAuthenticated, searchParams, setSearchParams]);

  useEffect(() => {
    if (!isAuthenticated || searchParams.get("phone") !== "1") {
      return;
    }
    const returnTo = searchParams.get("returnTo");
    const target = returnTo ? resolveSafeReturnTo(returnTo) : null;
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.delete("phone");
    nextSearchParams.delete("returnTo");
    setSearchParams(nextSearchParams, { replace: true });
    if (user?.phoneVerified) {
      if (target) navigate(target, { replace: true });
      return;
    }
    setPhoneReturnTo(target);
    setPhoneOnboardingHandled(false);
    setShowPhoneRegistration(true);
  }, [isAuthenticated, navigate, searchParams, setSearchParams, user?.phoneVerified]);

  const handleOpenClaim = () => {
    if (!user?.phone) {
      setPhoneOnboardingHandled(false);
      setShowPhoneRegistration(true);
      return;
    }
    setShowClaimModal(true);
  };

  const closePhoneRegistration = () => {
    setPhoneOnboardingHandled(true);
    setShowPhoneRegistration(false);
    setPhoneStepUpCompleted(false);
    setPhoneReturnTo(null);
  };

  return (
    <>
      <MyDashboardHero user={user!} nextBooking={nextBooking} />

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

      {passesLoading && <LoadingSpinner text="이용권 현황을 확인하고 있습니다" />}
      <ErrorAlert error={passesError} onRetry={() => { void refetchPasses(); }} retrying={passesFetching} />

      <div className="my-recent-grid">
        <MyBookingsSection
          bookings={bookings}
          isLoading={bookingsLoading}
          error={bookingsError}
          isFetching={bookingsFetching}
          onRetry={() => void refetchBookings()}
        />
        <MyOrdersSection
          orders={orders}
          isLoading={ordersLoading}
          error={ordersError}
          isFetching={ordersFetching}
          onRetry={() => void refetchOrders()}
        />
      </div>

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

      {showClaimModal && user!.phone && (
        <GuestClaimModal
          onClose={() => setShowClaimModal(false)}
          phone={user!.phone}
          phoneVerified={user!.phoneVerified}
          onPhoneVerified={async () => {
            await refresh();
          }}
        />
      )}

      <MemberPhoneUpdateModal
        show={showPhoneRegistration}
        currentPhone={user!.phone}
        localPasswordEnabled={user!.localPasswordEnabled}
        initiallyReauthenticated={phoneStepUpCompleted}
        returnTo={phoneReturnTo}
        onClose={closePhoneRegistration}
        onUpdated={async () => {
          await refresh();
          if (phoneReturnTo) navigate(phoneReturnTo, { replace: true });
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
    </>
  );
}
