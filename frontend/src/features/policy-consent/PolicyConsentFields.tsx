import { Form } from "react-bootstrap";
import { Link } from "react-router-dom";
import { ErrorAlert } from "@/shared/ui";
import type { CurrentPolicyConsent } from "./types";

interface Props {
  id: string;
  policy?: CurrentPolicyConsent;
  checked: boolean;
  onChange: (checked: boolean) => void;
  isLoading: boolean;
  error: unknown;
}

export function PolicyConsentFields({
  id,
  policy,
  checked,
  onChange,
  isLoading,
  error,
}: Props) {
  return (
    <div className="mb-3">
      <ErrorAlert error={error} />
      <Form.Check
        id={id}
        type="checkbox"
        checked={checked}
        disabled={!policy || isLoading}
        onChange={(event) => onChange(event.target.checked)}
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
