import { clientOnlyMeta } from "@/shared/seo/metadata";

export { OrderCreatePage as default } from "@/pages/OrderCreatePage";

export function meta() {
  return clientOnlyMeta("주문서");
}
