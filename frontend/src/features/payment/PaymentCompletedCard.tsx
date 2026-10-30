import type { ConfirmPaymentResponse } from "@/generated/api/payment";
import { PaymentCompletionNext } from "./PaymentCompletionNext";
import { PaymentReceiptLink } from "./PaymentReceiptLink";
import { PaymentResultCard } from "./PaymentResultCard";

/** 결제 직후 상태만 설명한다. 주문은 공방 승인 전이므로 확정·발송을 약속하지 않는다. */
const PAYMENT_RESULT_LEAD: Record<ConfirmPaymentResponse["context"], string> = {
  ORDER: "주문이 접수되어 공방 승인을 기다리고 있습니다. 진행 상황은 주문 상세에서 확인할 수 있습니다.",
  BOOKING: "예약이 확정되었습니다. 수업 당일 공방에서 뵙겠습니다.",
  PASS: "4회권이 발급되었습니다. 이제 이용권으로 정규 공예 수업을 예약할 수 있습니다.",
};

/** 결제 완료 카드. 결제 완료 화면과, 결제창 취소 뒤 실제로는 결제가 끝난 것을 확인한 실패 화면이 함께 쓴다. */
export function PaymentCompletedCard({ result }: { result: ConfirmPaymentResponse }) {
  return (
    <PaymentResultCard
      tone="success"
      title="결제 완료"
      lead={PAYMENT_RESULT_LEAD[result.context]}
      actions={(
        <>
          <PaymentCompletionNext result={result} />
          <PaymentReceiptLink receiptUrl={result.receiptUrl} size="default" />
        </>
      )}
    />
  );
}
