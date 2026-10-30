import { GROUP_INQUIRY_STATUS, type GroupInquiryStatus } from "@/features/group-inquiry/status";
import { ToneBadge, type CustomerBadgeTone } from "@/shared/ui";

// 공방과 상담·진행 중인 문의는 먹색, 끝난 문의(종료·고객 취소)는 회색 면이다.
const GROUP_INQUIRY_TONE: Record<GroupInquiryStatus, CustomerBadgeTone> = {
  RECEIVED: "progress",
  CONSULTING: "progress",
  CONFIRMED: "progress",
  CLOSED: "done",
  CANCELED: "done",
};

export function GroupInquiryStatusBadge({ status }: { status: GroupInquiryStatus }) {
  return (
    <ToneBadge tone={GROUP_INQUIRY_TONE[status] ?? "done"}>
      {GROUP_INQUIRY_STATUS[status] ?? "상태 확인 필요"}
    </ToneBadge>
  );
}
