import { MySectionPage } from "@/features/my/MySectionPage";
import { MyFavoritesSection } from "@/features/my/Favorites";

export function MyFavoritesPage() {
  return <MySectionPage path="/my/favorites"><MyFavoritesSection /></MySectionPage>;
}
