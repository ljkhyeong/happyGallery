import { myNavLabel } from "@/features/my/myNavigation";
import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyPassesPage as default } from "@/pages/MyPassesPage";

export function meta() {
  return clientOnlyMeta(myNavLabel("/my/passes"));
}
