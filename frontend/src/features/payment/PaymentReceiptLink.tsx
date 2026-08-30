export function PaymentReceiptLink({ receiptUrl }: { receiptUrl: string | null }) {
  if (!receiptUrl) return null;
  return (
    <a className="btn btn-outline-secondary btn-sm" href={receiptUrl} target="_blank" rel="noreferrer">
      결제 영수증 보기
    </a>
  );
}
