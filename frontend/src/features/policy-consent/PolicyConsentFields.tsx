import { Form } from "react-bootstrap";
import { Link } from "react-router";
import { ErrorAlert } from "@/shared/ui";
import type { usePolicyAcceptance } from "./usePolicyAcceptance";

interface Props {
  id: string;
  consent: Pick<ReturnType<typeof usePolicyAcceptance>, "policyQuery" | "accepted" | "setAccepted">;
  /** 제출 중처럼 화면이 동의 변경을 막아야 할 때 쓴다. */
  disabled?: boolean;
}

export function PolicyConsentFields({ id, consent, disabled = false }: Props) {
  const { data: policy, isLoading, error } = consent.policyQuery;
  return (
    <div className="mb-3">
      <ErrorAlert error={error} />
      <Form.Check
        id={id}
        type="checkbox"
        checked={consent.accepted}
        disabled={!policy || isLoading || disabled}
        onChange={(event) => consent.setAccepted(event.target.checked)}
        label={(
          <span className="small">
            [필수]{" "}
            <Link to={policy?.terms.documentPath ?? "/terms"} target="_blank" rel="noreferrer">
              이용약관
            </Link>
            과{" "}
            <Link to={policy?.privacy.documentPath ?? "/privacy"} target="_blank" rel="noreferrer">
              개인정보처리방침
            </Link>
            을 확인하고 동의합니다.
          </span>
        )}
      />
    </div>
  );
}
