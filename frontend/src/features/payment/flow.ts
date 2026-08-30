import { preparePayment } from "./api";
import {
  removePaymentReturnHint,
  removePaymentStatusToken,
  storePaymentReturnHint,
  storePaymentStatusToken,
  type PaymentSessionHandle,
  type PaymentReturnHint,
} from "./session";
import { requestTossPayment } from "./TossCheckout";
import { requireCheckoutTerms, type CheckoutSelection } from "./checkoutSelection";
import {
  captureCustomerSession,
  CustomerSessionChangedError,
  requireCurrentCustomerSession,
} from "@/shared/api";
import type { PaymentContext, PaymentPayload, PreparePaymentResponse } from "./types";

interface ExecutePaymentFlowArgs<T extends PaymentPayload> {
  checkoutSelection?: CheckoutSelection;
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
  onZeroAmount?: (
    prep: PreparePaymentResponse,
    requireCurrentCustomer: () => void,
  ) => Promise<void> | void;
}

/**
 * 결제 prepare → (0원이면 onZeroAmount, 아니면 Toss redirect) 흐름을 한 곳에 모은다.
 * 페이지마다 흩어져 있던 분기를 줄여 결제 정책 변경 시 수정 지점을 단일화한다.
 */
export async function executePaymentFlow<T extends PaymentPayload>(
  args: ExecutePaymentFlowArgs<T>,
): Promise<void> {
  requireCheckoutTerms(args.checkoutSelection);
  const customerSession = captureCustomerSession();
  const requireCurrentCustomer = () =>
    requireCurrentCustomerSession(customerSession);
  const runCustomerStep = async <R>(
    operation: () => R | Promise<R>,
  ): Promise<R> => {
    requireCurrentCustomer();
    try {
      const result = await operation();
      requireCurrentCustomer();
      return result;
    } catch (error) {
      requireCurrentCustomer();
      throw error;
    }
  };
  const runCustomerEffect = <R>(
    effect: () => R,
    rollback?: (result: R) => void,
  ): R => {
    requireCurrentCustomer();
    const result = effect();
    try {
      requireCurrentCustomer();
      return result;
    } catch (error) {
      rollback?.(result);
      throw error;
    }
  };

  let storedStatusToken: {
    orderId: string;
    handle: PaymentSessionHandle<string>;
  } | null = null;
  let storedReturnHint: PaymentSessionHandle<PaymentReturnHint> | null = null;
  try {
    const prep = await runCustomerStep(() =>
      preparePayment(args.context, args.payload));
    const statusTokenHandle = runCustomerEffect(
      () => storePaymentStatusToken(
        prep.orderId,
        prep.statusToken,
        customerSession,
      ),
      (handle) => {
        if (handle) removePaymentStatusToken(prep.orderId, handle);
      },
    );
    if (statusTokenHandle) {
      storedStatusToken = {
        orderId: prep.orderId,
        handle: statusTokenHandle,
      };
    }

    if (prep.amount === 0) {
      if (!args.onZeroAmount) {
        throw new Error("0원 결제 응답을 처리할 수 없는 컨텍스트입니다.");
      }
      await runCustomerStep(() =>
        args.onZeroAmount?.(prep, requireCurrentCustomer));
      return;
    }

    const returnHint = args.returnHint;
    if (returnHint) {
      storedReturnHint = runCustomerEffect(
        () => storePaymentReturnHint(returnHint, customerSession),
        (handle) => {
          if (handle) removePaymentReturnHint(handle);
        },
      );
    }
    const orderName = runCustomerEffect(() =>
      typeof args.orderName === "function" ? args.orderName(prep) : args.orderName);
    await runCustomerStep(() =>
      requestTossPayment({
        checkoutMethod: args.checkoutSelection?.method,
        orderId: prep.orderId,
        amount: prep.amount,
        orderName,
        customerKey: args.customerKey,
        customerName: args.customerName,
        customerMobilePhone: args.customerPhone,
      }, requireCurrentCustomer));
  } catch (error) {
    if (error instanceof CustomerSessionChangedError) {
      if (storedStatusToken) {
        removePaymentStatusToken(
          storedStatusToken.orderId,
          storedStatusToken.handle,
        );
      }
      if (storedReturnHint) {
        removePaymentReturnHint(storedReturnHint);
      }
    }
    throw error;
  }
}
