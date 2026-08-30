import { abandonPayment, preparePayment } from "./api";
import {
  readPaymentConfirmSession,
  removePaymentConfirmRequest,
  removePaymentReturnHint,
  removePaymentStatusToken,
  storePaymentReturnHint,
  storePaymentConfirmRequest,
  storePaymentStatusToken,
  type PaymentSessionHandle,
  type PaymentReturnHint,
  type PaymentConfirmRequest,
} from "./session";
import { requestTossPayment } from "./TossCheckout";
import { requireCheckoutTerms, type CheckoutSelection } from "./checkoutSelection";
import {
  captureCustomerSession,
  CustomerSessionChangedError,
  requireCurrentCustomerSession,
} from "@/shared/api";
import type { PaymentContext, PaymentPayload, PreparePaymentResponse } from "./types";

export class PaymentRecoveryStorageError extends Error {
  constructor() {
    super("브라우저에 결제 복구 정보를 저장할 수 없습니다. 저장소 설정을 확인한 뒤 다시 시도해 주세요.");
  }
}

interface ExecutePaymentFlowArgs<T extends PaymentPayload> {
  checkoutSelection?: CheckoutSelection;
  context: PaymentContext;
  payload: T;
  orderName: string | ((prep: PreparePaymentResponse) => string);
  customerKey?: string;
  customerName?: string;
  customerPhone?: string;
  returnHint?: PaymentReturnHint;
}

/**
 * 결제 prepare → (0원이면 공통 확정 화면, 아니면 Toss redirect) 흐름을 한 곳에 모은다.
 * 페이지마다 흩어져 있던 분기를 줄여 결제 정책 변경 시 수정 지점을 단일화한다.
 */
export async function executePaymentFlow<T extends PaymentPayload>(
  args: ExecutePaymentFlowArgs<T>,
): Promise<void> {
  const customerSession = captureCustomerSession();
  if (readPaymentConfirmSession(customerSession)?.value.amount === 0) {
    requireCurrentCustomerSession(customerSession);
    window.location.assign("/payments/success");
    return;
  }
  requireCheckoutTerms(args.checkoutSelection);
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
  let storedConfirmRequest: PaymentSessionHandle<PaymentConfirmRequest> | null = null;
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

    const returnHint = args.returnHint;
    if (returnHint) {
      storedReturnHint = runCustomerEffect(
        () => storePaymentReturnHint({ ...returnHint, orderId: prep.orderId }, customerSession),
        (handle) => {
          if (handle) removePaymentReturnHint(handle);
        },
      );
    }
    if (prep.amount === 0) {
      storedConfirmRequest = runCustomerEffect(
        () => storePaymentConfirmRequest({ paymentKey: null, orderId: prep.orderId, amount: 0 }, customerSession),
        (handle) => { if (handle) removePaymentConfirmRequest(handle); },
      );
      if (!storedConfirmRequest) {
        await runCustomerStep(() => abandonPayment(prep.orderId, prep.statusToken));
        throw new PaymentRecoveryStorageError();
      }
      runCustomerEffect(() => window.location.assign("/payments/success"));
      return;
    }
    const orderName = runCustomerEffect(() =>
      typeof args.orderName === "function" ? args.orderName(prep) : args.orderName);
    try {
      await runCustomerStep(() => requestTossPayment({
        checkoutMethod: args.checkoutSelection?.method,
        orderId: prep.orderId,
        amount: prep.amount,
        orderName,
        customerKey: args.customerKey,
        customerName: args.customerName,
        customerMobilePhone: args.customerPhone,
      }, requireCurrentCustomer));
    } catch (error) {
      requireCurrentCustomer();
      try {
        await runCustomerStep(() => abandonPayment(prep.orderId, prep.statusToken));
      } catch {
        requireCurrentCustomer();
        // 승인 진행·종료 응답 유실 시 조회 자격을 남기고 기존 오류를 표시한다.
      }
      throw error;
    }
  } catch (error) {
    if (error instanceof CustomerSessionChangedError) {
      if (storedConfirmRequest) removePaymentConfirmRequest(storedConfirmRequest);
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
