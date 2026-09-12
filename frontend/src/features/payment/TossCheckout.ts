import { tossCheckoutOptions, type CheckoutMethod } from "./checkoutSelection";

const TOSS_SDK_URL = "https://js.tosspayments.com/v2/standard";
const TOSS_SDK_LOAD_TIMEOUT_MS = 10_000;

let sdkPromise: Promise<TossPaymentsCtor> | null = null;

type TossPaymentsCtor = (clientKey: string) => TossPaymentsInstance;

interface TossPaymentsInstance {
  payment: (opts: { customerKey: string }) => TossPaymentInstance;
}

interface TossPaymentInstance {
  requestPayment: (opts: TossRequestPaymentArgs) => Promise<unknown>;
}

interface TossRequestPaymentArgs {
  method: "CARD";
  amount: { currency: "KRW"; value: number };
  orderId: string;
  orderName: string;
  successUrl: string;
  failUrl: string;
  customerName?: string;
  customerMobilePhone?: string;
  card?: { flowMode: "DIRECT"; easyPay: Exclude<CheckoutMethod, "DEFAULT"> };
  windowTarget?: "self";
}

declare global {
  interface Window {
    TossPayments?: TossPaymentsCtor;
  }
}

function loadTossSdk(): Promise<TossPaymentsCtor> {
  if (window.TossPayments) return Promise.resolve(window.TossPayments);
  return sdkPromise ??= new Promise<TossPaymentsCtor>((resolve, reject) => {
    const script = document.createElement("script");
    const cleanup = () => {
      window.clearTimeout(timeout);
      script.onload = null;
      script.onerror = null;
    };
    const fail = () => {
      cleanup();
      script.remove();
      reject(new Error("결제창을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."));
    };
    const timeout = window.setTimeout(fail, TOSS_SDK_LOAD_TIMEOUT_MS);
    script.src = TOSS_SDK_URL;
    script.async = true;
    script.onload = () => {
      if (!window.TossPayments) {
        fail();
        return;
      }
      cleanup();
      resolve(window.TossPayments);
    };
    script.onerror = fail;
    document.head.appendChild(script);
  }).catch((error: unknown) => {
    sdkPromise = null;
    throw error;
  });
}

export interface RequestTossPaymentArgs {
  checkoutMethod?: CheckoutMethod;
  orderId: string;
  amount: number;
  orderName: string;
  customerKey?: string;
  customerName?: string;
  customerMobilePhone?: string;
  successPath?: string;
  failPath?: string;
}

export async function requestTossPayment(
  args: RequestTossPaymentArgs,
  requireAllowed: () => void = () => undefined,
): Promise<void> {
  const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;
  if (!clientKey) {
    throw new Error("VITE_TOSS_CLIENT_KEY 환경 변수가 설정되지 않았습니다.");
  }
  const TossPaymentsCtorFn = await loadTossSdk();
  requireAllowed();
  const tossPayments = TossPaymentsCtorFn(clientKey);
  const customerKey = args.customerKey ?? generateAnonymousCustomerKey();
  const payment = tossPayments.payment({ customerKey });
  requireAllowed();
  await payment.requestPayment({
    ...tossCheckoutOptions(args.checkoutMethod),
    method: "CARD",
    amount: { currency: "KRW", value: args.amount },
    orderId: args.orderId,
    orderName: args.orderName,
    successUrl: `${window.location.origin}${args.successPath ?? "/payments/success"}`,
    failUrl: `${window.location.origin}${args.failPath ?? "/payments/fail"}`,
    customerName: args.customerName,
    customerMobilePhone: args.customerMobilePhone,
  });
}

function generateAnonymousCustomerKey(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return `anon_${crypto.randomUUID()}`;
  }
  return `anon_${Date.now()}_${Math.random().toString(36).slice(2)}`;
}
