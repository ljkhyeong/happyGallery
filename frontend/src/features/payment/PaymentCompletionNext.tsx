import type { ReactNode } from "react";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { LinkButton } from "@/shared/ui";
import type { ConfirmPaymentResponse } from "./types";

function GuestCompletionActions({ children }: { children: ReactNode }) {
  const signupHref = buildAuthPageHref("/signup", {
    redirectTo: "/my?claim=1",
    claim: true,
  });

  return (
    <div className="d-flex flex-wrap gap-2">
      {children}
      <LinkButton to={signupHref} variant="outline-dark">
        회원가입하고 이력 가져오기
      </LinkButton>
    </div>
  );
}

export function PaymentCompletionNext({ result }: { result: ConfirmPaymentResponse }) {
  if (result.domainId == null) {
    const nextAction = (
      <LinkButton to={result.accessRecoveryRequired ? "/guest" : "/"} variant="primary">
        {result.accessRecoveryRequired ? "휴대폰 인증으로 조회하기" : "홈으로"}
      </LinkButton>
    );
    return result.accessRecoveryRequired
      ? <GuestCompletionActions>{nextAction}</GuestCompletionActions>
      : nextAction;
  }
  if (result.context === "PASS") {
    return (
      <LinkButton to="/my/passes" variant="primary">
        내 8회권 확인하기
      </LinkButton>
    );
  }
  if (result.context === "ORDER") {
    if (result.accessToken) {
      return (
        <GuestCompletionActions>
          <LinkButton
            to="/guest/orders"
            state={{ orderId: result.domainId, token: result.accessToken }}
            variant="primary"
          >
            비회원 주문 확인하기
          </LinkButton>
        </GuestCompletionActions>
      );
    }
    if (result.accessRecoveryRequired) {
      return (
        <GuestCompletionActions>
          <LinkButton to="/guest" variant="primary">
            휴대폰 인증으로 주문 확인하기
          </LinkButton>
        </GuestCompletionActions>
      );
    }
    return (
      <LinkButton to={`/my/orders/${result.domainId}`} variant="primary">
        내 주문 상세 보기
      </LinkButton>
    );
  }
  if (result.accessToken) {
    return (
      <GuestCompletionActions>
        <LinkButton
          to="/guest/bookings"
          state={{ bookingId: result.domainId, token: result.accessToken }}
          variant="primary"
        >
          비회원 예약 확인하기
        </LinkButton>
      </GuestCompletionActions>
    );
  }
  if (result.accessRecoveryRequired) {
    return (
      <GuestCompletionActions>
        <LinkButton to="/guest" variant="primary">
          휴대폰 인증으로 예약 확인하기
        </LinkButton>
      </GuestCompletionActions>
    );
  }
  return (
    <LinkButton to={`/my/bookings/${result.domainId}`} variant="primary">
      내 예약 상세 보기
    </LinkButton>
  );
}
