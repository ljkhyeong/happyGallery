import { myNavLabel } from "@/features/my/myNavigation";
import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyRestockAlertsPage as default } from "@/pages/MyRestockAlertsPage";

export function meta() {
  return clientOnlyMeta(myNavLabel("/my/restock-alerts"));
}
