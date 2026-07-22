import { defineConfig } from "orval";

export default defineConfig({
  productApi: {
    input: {
      target: "../docs/PRD/0004_API_계약/openapi3.json",
      filters: {
        tags: ["product-controller"],
      },
    },
    output: {
      target: "./src/generated/api/product.ts",
      client: "fetch",
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
  },
  customerAccountApi: {
    input: {
      target: "../docs/PRD/0004_API_계약/openapi3.json",
      filters: {
        tags: ["me-social-account-controller"],
      },
    },
    output: {
      target: "./src/generated/api/customerAccount.ts",
      client: "fetch",
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
  },
  notificationApi: {
    input: {
      target: "../docs/PRD/0004_API_계약/openapi3.json",
      filters: {
        tags: ["me-notification-controller"],
      },
    },
    output: {
      target: "./src/generated/api/notification.ts",
      client: "fetch",
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
  },
  paymentQueryApi: {
    input: {
      target: "../docs/PRD/0004_API_계약/openapi3.json",
      filters: {
        tags: ["payment-query-controller"],
      },
    },
    output: {
      target: "./src/generated/api/paymentQuery.ts",
      client: "fetch",
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
  },
  guestRecordRecoveryApi: {
    input: {
      target: "../docs/PRD/0004_API_계약/openapi3.json",
      filters: {
        tags: ["guest-record-recovery-controller"],
      },
    },
    output: {
      target: "./src/generated/api/guestRecordRecovery.ts",
      client: "fetch",
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
  },
  workshopApi: {
    input: {
      target: "../docs/PRD/0004_API_계약/openapi3.json",
      filters: {
        tags: ["workshop-profile-controller", "admin-workshop-profile-controller"],
      },
    },
    output: {
      target: "./src/generated/api/workshop.ts",
      client: "fetch",
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
  },
});
