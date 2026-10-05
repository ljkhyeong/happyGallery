import { MySectionPage } from "@/features/my/MySectionPage";
import { MyGroupInquiriesSection } from "@/features/my/MyGroupInquiriesSection";

export function MyGroupInquiriesPage() {
  return <MySectionPage path="/my/group-inquiries" description="접수한 단체 수업 문의와 상담 상태를 확인합니다."><MyGroupInquiriesSection /></MySectionPage>;
}
