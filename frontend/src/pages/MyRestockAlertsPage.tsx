import { MySectionPage } from "@/features/my/MySectionPage";
import { MyRestockAlertsSection } from "@/features/my/MyRestockAlertsSection";

export function MyRestockAlertsPage() {
  return <MySectionPage path="/my/restock-alerts" description="품절 작품이 다시 입고되면 한 번 알려 드립니다."><MyRestockAlertsSection /></MySectionPage>;
}
