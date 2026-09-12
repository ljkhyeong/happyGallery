import { formatDate } from "@/shared/lib";
import { ErrorAlert } from "@/shared/ui";
import { useWorkshopProfile } from "./useWorkshopProfile";
import { NaverTalkInquiry } from "./NaverTalkInquiry";

interface Props {
  className: string;
  desiredDate: string;
}

export function WorkshopInquiryLink({ className, desiredDate }: Props) {
  const {
    data: workshop,
    error,
    query: { isFetching, refetch },
  } = useWorkshopProfile();
  if (!workshop && error) {
    return (
      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching}
      />
    );
  }
  if (!workshop?.naverTalkUrl) return null;

  const message = [
    "해피갤러리 클래스 일정 문의드립니다.",
    `클래스: ${className}`,
    `희망일: ${desiredDate ? formatDate(desiredDate) : "날짜 협의"}`,
  ].join("\n");

  return (
    <div className="mt-3">
      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching}
      />
      <NaverTalkInquiry message={message} url={workshop.naverTalkUrl} />
    </div>
  );
}
