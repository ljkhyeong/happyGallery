import { useState } from "react";
import { Button, Form } from "react-bootstrap";
import { Copy, MessageCircleMore } from "lucide-react";

interface Props {
  message: string;
  url: string;
  label?: string;
}

export function NaverTalkInquiry(props: Props) {
  return <InquiryActions key={props.message} {...props} />;
}

function InquiryActions({ message, url, label = "네이버톡톡 문의" }: Props) {
  const [copyStatus, setCopyStatus] = useState<"idle" | "copying" | "copied" | "failed">("idle");

  const copyMessage = async () => {
    setCopyStatus("copying");
    try {
      await navigator.clipboard.writeText(message);
      setCopyStatus("copied");
    } catch {
      setCopyStatus("failed");
    }
  };

  return (
    <div>
      <p className="small text-muted-soft mb-2">문의 문구를 복사한 뒤 네이버톡톡에 붙여넣어 주세요.</p>
      <div className="d-flex flex-wrap gap-2">
        <Button type="button" variant="outline-secondary" size="sm"
          onClick={copyMessage} disabled={copyStatus === "copying"}>
          <Copy size={15} aria-hidden="true" className="me-1" />
          {copyStatus === "copying" ? "복사 중..." : "문의 문구 복사"}
        </Button>
        <Button as="a" role="link" href={url} target="_blank" rel="noopener noreferrer"
          variant="outline-success" size="sm">
          <MessageCircleMore size={15} aria-hidden="true" className="me-1" />
          {label}
        </Button>
      </div>
      {copyStatus === "copied" && (
        <p role="status" className="small text-success mt-2 mb-0">문의 문구를 복사했습니다.</p>
      )}
      {copyStatus === "failed" && (
        <div className="mt-2">
          <p role="alert" className="small text-danger mb-2">복사하지 못했습니다. 아래 문구를 직접 선택해 복사해 주세요.</p>
          <Form.Control as="textarea" rows={4} readOnly value={message} aria-label="직접 복사할 문의 문구"
            onFocus={(event) => event.currentTarget.select()} />
        </div>
      )}
    </div>
  );
}
