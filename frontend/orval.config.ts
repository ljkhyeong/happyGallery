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
      clean: true,
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
