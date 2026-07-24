import { defineConfig } from "orval";

const OPENAPI_SPEC = "../docs/PRD/0004_API_계약/openapi3.json";

function generatedApi(target: string, tags: string[]) {
  return {
    input: {
      target: OPENAPI_SPEC,
      filters: { tags },
    },
    output: {
      target,
      client: "fetch" as const,
      clean: false,
      override: {
        header: false,
        fetch: {
          includeHttpResponseReturnType: false,
        },
        mutator: {
          path: "./src/shared/api/generatedClient.ts",
          name: "generatedApiClient",
        },
      },
    },
  };
}

export default defineConfig({
  productApi: generatedApi("./src/generated/api/product.ts", ["product-controller"]),
  customerAccountApi: generatedApi("./src/generated/api/customerAccount.ts", [
    "me-social-account-controller",
  ]),
  notificationApi: generatedApi("./src/generated/api/notification.ts", [
    "me-notification-controller",
  ]),
  paymentQueryApi: generatedApi("./src/generated/api/paymentQuery.ts", [
    "payment-query-controller",
  ]),
  guestRecordRecoveryApi: generatedApi("./src/generated/api/guestRecordRecovery.ts", [
    "guest-record-recovery-controller",
  ]),
  workshopApi: generatedApi("./src/generated/api/workshop.ts", [
    "workshop-profile-controller",
    "admin-workshop-profile-controller",
  ]),
  adminBookingApi: generatedApi("./src/generated/api/adminBooking.ts", [
    "admin-booking-controller",
  ]),
  orderClaimApi: generatedApi("./src/generated/api/orderClaim.ts", [
    "me-order-claim-controller",
    "guest-order-claim-controller",
    "admin-order-claim-controller",
  ]),
  policyConsentApi: generatedApi("./src/generated/api/policyConsent.ts", [
    "policy-consent-controller",
  ]),
  productQnaApi: generatedApi("./src/generated/api/productQna.ts", [
    "product-qna-controller",
    "me-product-qna-controller",
    "admin-product-qna-controller",
  ]),
});
