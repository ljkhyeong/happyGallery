import { telHref } from "@/shared/lib";
import { useWorkshopProfile } from "./useWorkshopProfile";

/** "공방에 문의해 주세요" 안내 옆에 두는 연락 수단. 공방 정보를 못 불러오거나 연락처가 없으면 그리지 않는다. */
export function WorkshopContactLinks({ className = "d-flex flex-wrap gap-2 mt-2" }: { className?: string }) {
  const { data: workshop } = useWorkshopProfile();
  if (!workshop?.phone && !workshop?.naverTalkUrl) return null;
  return (
    <div className={className}>
      {workshop.phone && (
        <a className="btn btn-outline-dark btn-sm" href={telHref(workshop.phone)}>전화 {workshop.phone}</a>
      )}
      {workshop.naverTalkUrl && (
        <a className="btn btn-outline-dark btn-sm" href={workshop.naverTalkUrl} target="_blank" rel="noopener noreferrer">
          네이버톡톡 문의
        </a>
      )}
    </div>
  );
}
