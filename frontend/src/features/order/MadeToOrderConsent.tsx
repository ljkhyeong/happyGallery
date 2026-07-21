import { useId } from "react";
import { Alert, Badge, Form } from "react-bootstrap";
import type { OrderPricePolicyResponse } from "@/shared/types";

interface Props {
  required: boolean;
  policy: OrderPricePolicyResponse | undefined;
  isLoading: boolean;
  isFetching: boolean;
  error: unknown;
  checked: boolean;
  onChange: (checked: boolean) => void;
  versionMismatch: boolean;
  refreshRequired: boolean;
}

export function MadeToOrderConsent({
  required,
  policy,
  isLoading,
  isFetching,
  error,
  checked,
  onChange,
  versionMismatch,
  refreshRequired,
}: Props) {
  const checkboxId = useId();
  if (!required) return null;
  const version = policy?.madeToOrderConsentVersion.trim() || null;

  return (
    <div className="border rounded p-3 mb-3">
      <div className="d-flex align-items-center gap-2 mb-2">
        <span className="fw-semibold">주문제작 상품 청약철회 제한 동의</span>
        <Badge bg="danger">필수</Badge>
      </div>

      {versionMismatch && (
        <Alert variant="warning" className="small py-2">
          {refreshRequired
            ? isFetching
              ? "변경된 동의 안내를 불러오는 중입니다. 잠시 후 새 내용을 확인해 주세요."
              : "변경된 동의 안내를 확인하지 못했습니다. 새로고침 후 다시 시도해 주세요."
            : "동의 안내가 변경되었습니다. 아래 새 내용을 확인한 뒤 다시 동의해 주세요."}
        </Alert>
      )}

      {isLoading && !policy && (
        <p className="small text-muted-soft mb-0">동의 내용을 확인하는 중입니다.</p>
      )}
      {error != null && !policy && (
        <Alert variant="danger" className="small py-2 mb-0">
          동의 내용을 불러오지 못했습니다. 새로고침 후 다시 시도해 주세요.
        </Alert>
      )}
      {policy && !version && (
        <Alert variant="danger" className="small py-2 mb-0">
          동의 기준을 확인하지 못했습니다. 새로고침 후 다시 시도해 주세요.
        </Alert>
      )}
      {policy && version && (
        <>
          <Form.Check
            id={checkboxId}
            type="checkbox"
            checked={checked}
            disabled={refreshRequired}
            onChange={(event) => onChange(event.target.checked)}
            label={policy.madeToOrderConsentText}
          />
          <Form.Text className="text-muted-soft">
            동의 기준 {version}
          </Form.Text>
        </>
      )}
    </div>
  );
}
