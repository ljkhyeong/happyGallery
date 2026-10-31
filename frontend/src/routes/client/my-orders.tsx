import { myNavLabel } from "@/features/my/myNavigation";
import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyOrdersPage as default } from "@/pages/MyOrdersPage";

export function meta() {
  return clientOnlyMeta(myNavLabel("/my/orders"));
}
