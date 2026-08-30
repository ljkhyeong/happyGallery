import { Alert } from "react-bootstrap";
import { ErrorAlert } from "@/shared/ui";
import { CheckoutTermsError } from "./checkoutSelection";

export function PaymentErrorAlert({ error }: { error: unknown }) {
  return error instanceof CheckoutTermsError
    ? <Alert variant="warning" role="alert">{error.message}</Alert>
    : <ErrorAlert error={error} />;
}
