import { Alert } from "react-bootstrap";
import { ErrorAlert } from "@/shared/ui";
import { CheckoutTermsError } from "./checkoutSelection";
import { PaymentRecoveryStorageError } from "./flow";

export function PaymentErrorAlert({ error }: { error: unknown }) {
  return error instanceof CheckoutTermsError || error instanceof PaymentRecoveryStorageError
    ? <Alert variant="warning" role="alert">{error.message}</Alert>
    : <ErrorAlert error={error} />;
}
