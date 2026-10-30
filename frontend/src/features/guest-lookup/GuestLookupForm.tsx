import { useState } from "react";
import { Button, Form } from "react-bootstrap";
import { parseGuestRecordNumber } from "./guestRecordNumber";

export type GuestLookupKind = "orders" | "bookings";

interface Props {
  kind: GuestLookupKind;
  onLookup: (id: number, token: string) => void;
  isLoading?: boolean;
  initialId?: string;
  initialToken?: string;
  submitLabel?: string;
}

export function GuestLookupForm({
  kind, onLookup, isLoading = false, initialId = "", initialToken = "", submitLabel,
}: Props) {
  const [id, setId] = useState(initialId);
  const [token, setToken] = useState(initialToken);
  const [touched, setTouched] = useState({ id: false, token: false });
  const label = kind === "orders" ? "주문" : "예약";
  const prefix = kind === "orders" ? "order-detail" : "booking-lookup";
  const parsedId = parseGuestRecordNumber(id, kind);
  const validId = parsedId !== null;
  const normalizedToken = token.trim();

  return (
    <Form className="guest-lookup-form" aria-busy={isLoading} onSubmit={(event) => {
      event.preventDefault();
      setTouched({ id: true, token: true });
      if (parsedId !== null && normalizedToken && !isLoading) onLookup(parsedId, normalizedToken);
    }}>
      <Form.Group controlId={`${prefix}-id`}>
        <Form.Label>{label} 번호</Form.Label>
        <Form.Control
          type="text" inputMode="numeric" value={id}
          onChange={(event) => setId(event.target.value)}
          onBlur={() => setTouched((current) => ({ ...current, id: true }))}
          // 화면·알림의 "주문 #123"에서 숫자만 넣어도, 번호 전체를 붙여 넣어도 된다.
          placeholder="예: 123"
          isInvalid={touched.id && !validId}
          aria-invalid={touched.id && !validId}
          aria-describedby={touched.id && !validId ? `${prefix}-id-error` : undefined}
        />
        <Form.Control.Feedback id={`${prefix}-id-error`} type="invalid">
          유효한 {label} 번호를 입력해 주세요.
        </Form.Control.Feedback>
      </Form.Group>
      <Form.Group controlId={`${prefix}-token`}>
        <Form.Label>조회 코드</Form.Label>
        <Form.Control
          value={token} autoComplete="off" spellCheck={false}
          // 조회 코드는 대소문자를 구분하므로 휴대폰의 첫 글자 대문자·자동 수정을 끈다.
          autoCapitalize="off" autoCorrect="off"
          onChange={(event) => setToken(event.target.value)}
          onBlur={() => setTouched((current) => ({ ...current, token: true }))}
          placeholder={`${label} 시 발급된 조회 코드`}
          isInvalid={touched.token && !normalizedToken}
          aria-invalid={touched.token && !normalizedToken}
          aria-describedby={touched.token && !normalizedToken ? `${prefix}-token-error` : undefined}
        />
        <Form.Control.Feedback id={`${prefix}-token-error`} type="invalid">
          조회 코드를 입력해 주세요.
        </Form.Control.Feedback>
      </Form.Group>
      <Button type="submit" variant="dark" className="w-100" disabled={!validId || !normalizedToken || isLoading}>
        {isLoading ? "조회 중..." : (submitLabel ?? `${label} 조회`)}
      </Button>
    </Form>
  );
}
