import { MySectionPage } from "@/features/my/MySectionPage";
import { MyFavoritesSection } from "@/features/my/Favorites";

export function MyFavoritesPage() {
  return <MySectionPage path="/my/favorites" description="상품·클래스 상세에서 찜한 항목을 모아 봅니다."><MyFavoritesSection /></MySectionPage>;
}
