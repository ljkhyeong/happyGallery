import { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { sendVerification } from "./api";
import { ErrorAlert } from "@/shared/ui";

interface Props {
  onVerified: (phone: string, code: string) => void;
  title?: string;
  description?: string;
  initialPhone?: string;
  lockPhone?: boolean;
  confirmLabel?: string;
}

export function PhoneVerificationStep({
  onVerified,
  title = "1. 휴대폰 인증",
  description,
  initialPhone = "",
  lockPhone = false,
  confirmLabel = "확인",
}: Props) {
  const [phone, setPhone] = useState(initialPhone.replace(/\D/g, ""));
  const [code, setCode] = useState("");
  const [sent, setSent] = useState(false);
  const [touched, setTouched] = useState(false);

  useEffect(() => {
    setPhone(initialPhone.replace(/\D/g, ""));
  }, [initialPhone]);

  const sendMutation = useMutation({
    mutationFn: () => sendVerification({ phone }),
    onSuccess: () => {
      setSent(true);
    },
  });

  const phoneValid = /^01[0-9]{8,9}$/.test(phone);
  const showPhoneError = touched && phone.length > 0 && !phoneValid;

  return (
    <div>
      <h6 className="mb-3">{title}</h6>
      {description && <p className="text-muted-soft small mb-3">{description}</p>}
      <ErrorAlert error={sendMutation.error} />

      <Row className="g-2 align-items-end mb-3">
        <Col xs={12} sm={8}>
          <Form.Group controlId="verification-phone">
            <Form.Label>휴대폰 번호</Form.Label>
            <Form.Control
              value={phone}
              onChange={(e) => setPhone(e.target.value.replace(/\D/g, ""))}
              onBlur={() => setTouched(true)}
              placeholder="01012345678"
              maxLength={11}
              disabled={sent || lockPhone}
              isInvalid={showPhoneError}
            />
            <Form.Control.Feedback type="invalid">
              010으로 시작하는 10~11자리 번호를 입력하세요.
            </Form.Control.Feedback>
          </Form.Group>
        </Col>
        <Col xs={12} sm={4}>
          <Button
            variant="outline-primary"
            className="w-100"
            disabled={!phoneValid || sendMutation.isPending}
            onClick={() => sendMutation.mutate()}
          >
            {sendMutation.isPending ? "발송 중..." : sent ? "재발송" : "인증코드 발송"}
          </Button>
        </Col>
      </Row>

      {sent && (
        <>
          <Row className="g-2 align-items-end">
            <Col xs={12} sm={8}>
              <Form.Group controlId="verification-code">
                <Form.Label>인증코드</Form.Label>
                <Form.Control
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="인증코드 입력"
                />
              </Form.Group>
            </Col>
            <Col xs={12} sm={4}>
              <Button
                variant="primary"
                className="w-100"
                disabled={!code.trim()}
                onClick={() => onVerified(phone, code.trim())}
              >
                {confirmLabel}
              </Button>
            </Col>
          </Row>
        </>
      )}
    </div>
  );
}
