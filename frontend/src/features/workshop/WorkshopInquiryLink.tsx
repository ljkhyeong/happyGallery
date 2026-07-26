import { Button } from "react-bootstrap";
import { MessageCircleMore } from "lucide-react";
import { formatDate } from "@/shared/lib";
import { useWorkshopProfile } from "./useWorkshopProfile";

interface Props {
  className: string;
  desiredDate: string;
}

export function WorkshopInquiryLink({ className, desiredDate }: Props) {
  const { data: workshop } = useWorkshopProfile();
  if (!workshop?.naverTalkUrl) return null;

  const message = [
    "해피갤러리 클래스 일정 문의드립니다.",
    `클래스: ${className}`,
    `희망일: ${desiredDate ? formatDate(desiredDate) : "날짜 협의"}`,
  ].join("\n");

  const copyInquiryContext = () => {
    void navigator.clipboard?.writeText(message);
  };

  return (
    <div className="mt-3">
      <p className="small text-muted-soft mb-2">
        아래 버튼을 누르면 문의 문구를 복사하고 네이버톡톡을 엽니다.
      </p>
      <Button
        as="a"
        href={workshop.naverTalkUrl}
        target="_blank"
        rel="noreferrer"
        variant="outline-success"
        size="sm"
        onClick={copyInquiryContext}
      >
        <MessageCircleMore size={15} aria-hidden="true" className="me-1" />
        문의 문구 복사 후 네이버톡톡 열기
      </Button>
    </div>
  );
}
