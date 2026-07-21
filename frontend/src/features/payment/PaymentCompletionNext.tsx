import { LinkButton } from "@/shared/ui";
import type { ConfirmPaymentResponse } from "./types";

export function PaymentCompletionNext({ result }: { result: ConfirmPaymentResponse }) {
  if (result.domainId == null) {
    return (
      <LinkButton to={result.accessRecoveryRequired ? "/guest" : "/"} variant="primary">
        {result.accessRecoveryRequired ? "휴대폰 인증으로 조회하기" : "홈으로"}
      </LinkButton>
    );
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
        <LinkButton
          to="/guest/orders"
          state={{ orderId: result.domainId, token: result.accessToken }}
          variant="primary"
        >
          비회원 주문 확인하기
        </LinkButton>
      );
    }
    if (result.accessRecoveryRequired) {
      return (
        <LinkButton to="/guest" variant="primary">
          휴대폰 인증으로 주문 확인하기
        </LinkButton>
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
      <LinkButton
        to="/guest/bookings"
        state={{ bookingId: result.domainId, token: result.accessToken }}
        variant="primary"
      >
        비회원 예약 확인하기
      </LinkButton>
    );
  }
  if (result.accessRecoveryRequired) {
    return (
      <LinkButton to="/guest" variant="primary">
        휴대폰 인증으로 예약 확인하기
      </LinkButton>
    );
  }
  return (
    <LinkButton to={`/my/bookings/${result.domainId}`} variant="primary">
      내 예약 상세 보기
    </LinkButton>
  );
}
