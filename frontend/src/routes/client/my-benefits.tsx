import { myNavLabel } from "@/features/my/myNavigation";
import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyBenefitsPage as default } from "@/pages/MyBenefitsPage";

export function meta() {
  return clientOnlyMeta(myNavLabel("/my/benefits"));
}
