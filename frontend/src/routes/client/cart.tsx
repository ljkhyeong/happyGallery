import { clientOnlyMeta } from "@/shared/seo/metadata";

export { CartPage as default } from "@/pages/CartPage";

export function meta() {
  return clientOnlyMeta("장바구니");
}
