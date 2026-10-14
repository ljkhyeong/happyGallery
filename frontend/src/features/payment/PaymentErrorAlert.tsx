import { Alert } from "react-bootstrap";
import { ErrorAlert } from "@/shared/ui";
import { CheckoutTermsError } from "./checkoutSelection";
import { PaymentRecoveryStorageError } from "./flow";

// 약관 미동의는 고객이 고치면 되는 확인 요청(노랑)이고, 결제 복구 정보를 저장할 수 없으면 결제를 막는 오류(빨강)다.
// 두 경우 모두 구체적인 안내 문구를 그대로 보여 준다(ErrorAlert는 일반 오류를 공통 문구로 바꾼다).
export function PaymentErrorAlert({ error }: { error: unknown }) {
  if (error instanceof CheckoutTermsError) return <Alert variant="warning" role="alert">{error.message}</Alert>;
  if (error instanceof PaymentRecoveryStorageError) return <Alert variant="danger" role="alert">{error.message}</Alert>;
  return <ErrorAlert error={error} />;
}
