import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Form, Button, Row, Col } from "react-bootstrap";
import { sendVerification } from "./api";
import { ErrorAlert } from "@/shared/ui";

interface Props {
  onVerified: (phone: string, code: string) => void;
}

export function PhoneVerificationStep({ onVerified }: Props) {
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [sent, setSent] = useState(false);
  const [mvpCode, setMvpCode] = useState("");

  const sendMutation = useMutation({
    mutationFn: () => sendVerification({ phone }),
    onSuccess: (res) => {
      setSent(true);
      setMvpCode(res.code); // MVP: 코드 노출
    },
  });

  const phoneValid = /^01[0-9]{8,9}$/.test(phone);

  return (
    <div>
      <h6 className="mb-3">1. 휴대폰 인증</h6>
      <ErrorAlert error={sendMutation.error} />

      <Row className="g-2 align-items-end mb-3">
        <Col xs={8}>
          <Form.Group>
            <Form.Label>휴대폰 번호</Form.Label>
            <Form.Control
              value={phone}
              onChange={(e) => setPhone(e.target.value.replace(/\D/g, ""))}
              placeholder="01012345678"
              maxLength={11}
              disabled={sent}
            />
          </Form.Group>
        </Col>
        <Col xs={4}>
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
          {mvpCode && (
            <p className="text-muted-soft small mb-2">
              [MVP] 인증코드: <strong>{mvpCode}</strong>
            </p>
          )}
          <Row className="g-2 align-items-end">
            <Col xs={8}>
              <Form.Group>
                <Form.Label>인증코드</Form.Label>
                <Form.Control
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                  placeholder="인증코드 입력"
                />
              </Form.Group>
            </Col>
            <Col xs={4}>
              <Button
                variant="primary"
                className="w-100"
                disabled={!code.trim()}
                onClick={() => onVerified(phone, code.trim())}
              >
                확인
              </Button>
            </Col>
          </Row>
        </>
      )}
    </div>
  );
}
