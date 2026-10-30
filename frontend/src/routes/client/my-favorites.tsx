import { myNavLabel } from "@/features/my/myNavigation";
import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyFavoritesPage as default } from "@/pages/MyFavoritesPage";

export function meta() {
  return clientOnlyMeta(myNavLabel("/my/favorites"));
}
