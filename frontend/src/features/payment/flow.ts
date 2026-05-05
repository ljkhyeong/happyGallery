import { preparePayment } from "./api";
import { storePaymentReturnHint, type PaymentReturnHint } from "./session";
import { requestTossPayment } from "./TossCheckout";
import type { PaymentContext, PaymentPayload, PreparePaymentResponse } from "./types";

interface ExecutePaymentFlowArgs<T extends PaymentPayload> {
  context: PaymentContext;
  payload: T;
  orderName: string | ((prep: PreparePaymentResponse) => string);
  customerKey?: string;
  customerName?: string;
  customerPhone?: string;
  returnHint?: PaymentReturnHint;
  /**
   * amount === 0 응답을 받았을 때 PG를 우회하고 직접 confirm으로 마무리하는 경로.
   * 8회권 사용 예약처럼 0원 결제가 정상 분기인 컨텍스트만 제공한다.
   */
  onZeroAmount?: (prep: PreparePaymentResponse) => Promise<void> | void;
}

/**
 * 결제 prepare → (0원이면 onZeroAmount, 아니면 Toss redirect) 흐름을 한 곳에 모은다.
 * 페이지마다 흩어져 있던 분기를 줄여 결제 정책 변경 시 수정 지점을 단일화한다.
 */
export async function executePaymentFlow<T extends PaymentPayload>(
  args: ExecutePaymentFlowArgs<T>,
): Promise<void> {
  const prep = await preparePayment(args.context, args.payload);

  if (prep.amount === 0) {
    if (!args.onZeroAmount) {
      throw new Error("0원 결제 응답을 처리할 수 없는 컨텍스트입니다.");
    }
    await args.onZeroAmount(prep);
    return;
  }

  if (args.returnHint) {
    storePaymentReturnHint(args.returnHint);
  }
  const orderName =
    typeof args.orderName === "function" ? args.orderName(prep) : args.orderName;
  await requestTossPayment({
    orderId: prep.orderId,
    amount: prep.amount,
    orderName,
    customerKey: args.customerKey,
    customerName: args.customerName,
    customerMobilePhone: args.customerPhone,
  });
}
