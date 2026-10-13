import { useId } from "react";
import { Form } from "react-bootstrap";

const CANCEL_REASON_MAX_LENGTH = 200;

interface Props {
  value: string;
  onChange: (value: string) => void;
  disabled: boolean;
  placeholder: string;
}

/** 공방 사정 취소(예약·회차)에서 고객에게 안내할 취소 사유를 받는다. */
export function CancelReasonField({ value, onChange, disabled, placeholder }: Props) {
  const controlId = useId();

  return (
    <Form.Group controlId={controlId}>
      <Form.Label>취소 사유</Form.Label>
      <Form.Control
        as="textarea"
        rows={3}
        maxLength={CANCEL_REASON_MAX_LENGTH}
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        autoFocus
      />
      <Form.Text className="text-muted">{value.length}/{CANCEL_REASON_MAX_LENGTH}자</Form.Text>
    </Form.Group>
  );
}
