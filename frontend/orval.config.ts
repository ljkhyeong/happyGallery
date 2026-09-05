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
    "social-signup-controller",
  ]),
  notificationApi: generatedApi("./src/generated/api/notification.ts", [
    "me-notification-controller",
  ]),
  paymentQueryApi: generatedApi("./src/generated/api/paymentQuery.ts", [
    "payment-query-controller",
  ]),
  roadAddressApi: generatedApi("./src/generated/api/roadAddress.ts", [
    "road-address-controller",
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
  bookingApi: generatedApi("./src/generated/api/booking.ts", [
    "booking-controller",
    "me-booking-controller",
    "booking-vacancy-alert-controller",
    "me-booking-vacancy-alert-controller",
    "class-controller",
    "slot-controller",
  ]),
  customerAuthApi: generatedApi("./src/generated/api/customerAuth.ts", [
    "csrf-controller",
    "customer-auth-controller",
    "customer-credential-controller",
    "me-account-controller",
    "me-email-controller",
    "me-phone-controller",
  ]),
  customerStoreApi: generatedApi("./src/generated/api/customerStore.ts", [
    "me-restock-alert-controller",
    "me-cart-controller",
    "me-guest-claim-controller",
    "me-inquiry-controller",
    "me-order-controller",
    "me-order-customer-action-controller",
    "me-pass-controller",
  ]),
  orderApi: generatedApi("./src/generated/api/order.ts", [
    "order-controller",
    "order-customer-action-controller",
  ]),
  paymentApi: generatedApi("./src/generated/api/payment.ts", [
    "payment-controller",
  ]),
  noticeApi: generatedApi("./src/generated/api/notice.ts", [
    "notice-controller",
  ]),
  eventApi: generatedApi("./src/generated/api/event.ts", [
    "event-controller",
  ]),
  memberBenefitApi: generatedApi("./src/generated/api/memberBenefit.ts", [
    "me-coupon-controller",
    "me-reward-controller",
  ]),
  monitoringApi: generatedApi("./src/generated/api/monitoring.ts", [
    "client-monitoring-controller",
  ]),
  adminAuthApi: generatedApi("./src/generated/api/adminAuth.ts", [
    "admin-login-controller",
    "admin-credential-controller",
    "admin-mfa-controller",
  ]),
  adminDashboardApi: generatedApi("./src/generated/api/adminDashboard.ts", [
    "admin-dashboard-controller",
  ]),
  adminNoticeApi: generatedApi("./src/generated/api/adminNotice.ts", [
    "admin-notice-controller",
  ]),
  adminEventApi: generatedApi("./src/generated/api/adminEvent.ts", [
    "admin-event-controller",
  ]),
  adminCouponApi: generatedApi("./src/generated/api/adminCoupon.ts", [
    "admin-coupon-controller",
  ]),
  adminCatalogApi: generatedApi("./src/generated/api/adminCatalog.ts", [
    "admin-stock-controller",
    "admin-class-controller",
    "admin-media-controller",
    "admin-product-controller",
    "admin-smart-store-notice-controller",
    "admin-slot-controller",
    "admin-slot-session-controller",
  ]),
  adminOrderApi: generatedApi("./src/generated/api/adminOrder.ts", [
    "admin-order-approval-controller",
    "admin-order-pickup-controller",
    "admin-order-production-controller",
    "admin-order-query-controller",
    "admin-order-shipping-controller",
    "admin-smart-store-order-controller",
  ]),
  adminOperationsApi: generatedApi("./src/generated/api/adminOperations.ts", [
    "admin-inquiry-controller",
    "admin-notification-controller",
    "admin-pass-controller",
    "admin-payment-reconciliation-controller",
    "admin-payment-settlement-controller",
    "admin-smart-store-settlement-controller",
    "admin-refund-controller",
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
    "admin-smart-store-inquiry-controller",
  ]),
  reviewApi: generatedApi("./src/generated/api/review.ts", [
    "product-review-controller",
    "class-review-controller",
    "me-review-controller",
    "me-review-image-media-controller",
    "admin-review-controller",
    "admin-review-image-media-controller",
    "admin-review-evidence-media-controller",
  ]),
});
