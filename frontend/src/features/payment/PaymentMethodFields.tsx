import { useId } from "react";
import { Form } from "react-bootstrap";
import type { CheckoutSelection } from "./checkoutSelection";

interface Props {
  value: CheckoutSelection;
  onChange: (value: CheckoutSelection) => void;
  disabled?: boolean;
}

export function PaymentMethodFields({ value, onChange, disabled }: Props) {
  const id = useId();
  return (
    <fieldset className="mb-3" disabled={disabled}>
      <legend className="fs-6 fw-semibold">결제수단</legend>
      <div className="d-flex flex-wrap gap-3 mb-2">
        <Form.Check
          type="radio" id={`${id}-default`} name={`${id}-method`}
          label="카드·간편결제" checked={value.method === "DEFAULT"}
          onChange={() => onChange({ method: "DEFAULT", termsAgreed: false })}
        />
        <Form.Check
          type="radio" id={`${id}-naverpay`} name={`${id}-method`}
          label="네이버페이" checked={value.method === "NAVERPAY"}
          onChange={() => onChange({ method: "NAVERPAY", termsAgreed: false })}
        />
      </div>
      {value.method === "NAVERPAY" ? (
        <>
          <p className="small text-muted mb-2">네이버페이 결제창으로 이동합니다. 전액 할인 시 결제창은 열리지 않습니다.</p>
          <Form.Check
            id={`${id}-terms`} label="[필수] 토스페이먼츠 결제 약관에 동의합니다."
            checked={value.termsAgreed}
            onChange={(event) => onChange({ ...value, termsAgreed: event.target.checked })}
          />
          <ul className="small ps-4 mt-1 mb-0">
            <li><a href="https://pages.tosspayments.com/terms/user" target="_blank" rel="noopener noreferrer">전자금융거래 이용약관</a></li>
            <li><a href="https://pages.tosspayments.com/terms/privacy/consent1" target="_blank" rel="noopener noreferrer">개인(신용)정보 수집·이용 동의</a></li>
            <li><a href="https://pages.tosspayments.com/terms/privacy/consent2" target="_blank" rel="noopener noreferrer">개인(신용)정보 제3자 제공 동의</a></li>
          </ul>
        </>
      ) : (
        <p className="small text-muted mb-0">토스 결제창에서 카드 또는 간편결제를 선택합니다.</p>
      )}
    </fieldset>
  );
}
