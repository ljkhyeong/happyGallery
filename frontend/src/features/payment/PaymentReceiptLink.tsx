interface Props {
  receiptUrl: string | null;
  /** 결제 결과 화면처럼 주요 버튼과 나란히 둘 때는 기본 크기를 쓴다. */
  size?: "sm" | "default";
}

export function PaymentReceiptLink({ receiptUrl, size = "sm" }: Props) {
  if (!receiptUrl) return null;
  return (
    <a
      className={size === "sm" ? "btn btn-outline-secondary btn-sm" : "btn btn-outline-secondary"}
      href={receiptUrl}
      target="_blank"
      rel="noreferrer"
    >
      결제 영수증 보기
    </a>
  );
}
