const TOSS_SDK_URL = "https://js.tosspayments.com/v2/standard";

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
}

declare global {
  interface Window {
    TossPayments?: TossPaymentsCtor;
  }
}

function loadTossSdk(): Promise<TossPaymentsCtor> {
  if (sdkPromise) return sdkPromise;
  sdkPromise = new Promise((resolve, reject) => {
    if (window.TossPayments) {
      resolve(window.TossPayments);
      return;
    }
    const script = document.createElement("script");
    script.src = TOSS_SDK_URL;
    script.async = true;
    script.onload = () => {
      if (window.TossPayments) resolve(window.TossPayments);
      else reject(new Error("Toss SDK 글로벌 객체를 찾지 못했습니다."));
    };
    script.onerror = () => {
      sdkPromise = null;
      reject(new Error("Toss SDK 스크립트 로드 실패"));
    };
    document.head.appendChild(script);
  });
  return sdkPromise;
}

export interface RequestTossPaymentArgs {
  orderId: string;
  amount: number;
  orderName: string;
  customerKey?: string;
  customerName?: string;
  customerMobilePhone?: string;
  successPath?: string;
  failPath?: string;
}

export async function requestTossPayment(args: RequestTossPaymentArgs): Promise<void> {
  const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY;
  if (!clientKey) {
    throw new Error("VITE_TOSS_CLIENT_KEY 환경 변수가 설정되지 않았습니다.");
  }
  const TossPaymentsCtorFn = await loadTossSdk();
  const tossPayments = TossPaymentsCtorFn(clientKey);
  const customerKey = args.customerKey ?? generateAnonymousCustomerKey();
  const payment = tossPayments.payment({ customerKey });
  await payment.requestPayment({
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
