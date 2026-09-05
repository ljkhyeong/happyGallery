import { useId, useState, type FormEvent } from "react";
import { Button, Col, Form, Row } from "react-bootstrap";
import type { GroupInquiryRequest } from "@/generated/api/customerStore";
import { ErrorAlert } from "@/shared/ui";

interface Props {
  onSubmit: (request: GroupInquiryRequest) => void;
  pending: boolean;
  error: unknown;
  initialContact?: { name: string; phone: string | null; email: string | null };
  submitLabel?: string;
}

export function GroupInquiryForm({ onSubmit, pending, error, initialContact, submitLabel = "단체 수업 문의 접수" }: Props) {
  const id = useId();
  const [form, setForm] = useState({ organization: "", contactName: initialContact?.name ?? "", phone: initialContact?.phone ?? "",
    email: initialContact?.email ?? "", headcount: "", preferredSchedule: "", location: "", classInterest: "", message: "" });
  const update = (key: keyof typeof form, value: string) => setForm((current) => ({ ...current, [key]: value }));
  const submit = (event: FormEvent) => {
    event.preventDefault();
    onSubmit({ ...form, headcount: Number(form.headcount), email: form.email.trim() || null, message: form.message.trim() || null });
  };
  const fields = [
    ["organization", "기관·모임명", "text", 200], ["contactName", "담당자 이름", "text", 100],
    ["phone", "담당자 휴대폰", "tel", 30], ["email", "이메일 (선택)", "email", 254],
    ["preferredSchedule", "희망 일정", "text", 200], ["location", "수업 장소", "text", 200],
    ["classInterest", "관심 수업", "text", 200],
  ] as const;
  return (
    <Form onSubmit={submit}>
      <fieldset disabled={pending}>
        <Row>
          {fields.map(([key, label, type, maxLength]) => (
            <Col md={6} key={key}>
              <Form.Group controlId={`${id}-${key}`} className="mb-3">
                <Form.Label>{label}</Form.Label>
                <Form.Control type={type} required={key !== "email"} maxLength={maxLength} value={form[key]}
                  placeholder={key === "preferredSchedule" ? "예: 10월 평일 오전 / 미정" : key === "location" ? "예: 기관 강당 / 공방 / 미정" : undefined}
                  onChange={(event) => update(key, event.target.value)} />
              </Form.Group>
            </Col>
          ))}
          <Col md={6}>
            <Form.Group controlId={`${id}-headcount`} className="mb-3">
              <Form.Label>참여 인원</Form.Label>
              <Form.Control type="number" required min={1} max={500} value={form.headcount} onChange={(event) => update("headcount", event.target.value)} />
            </Form.Group>
          </Col>
        </Row>
        <Form.Group controlId={`${id}-message`} className="mb-3">
          <Form.Label>추가 요청 (선택)</Form.Label>
          <Form.Control as="textarea" rows={3} maxLength={2000} value={form.message} onChange={(event) => update("message", event.target.value)}
            placeholder="참여 대상, 수업 시간, 예산 등 상담에 필요한 내용을 적어 주세요." />
        </Form.Group>
        <p className="small text-muted">담당자 연락처는 문의 응대에 사용합니다. 접수 후 연락드리며, 수업 일정과 비용은 상담을 통해 정합니다.</p>
        <ErrorAlert error={error} />
        <Button type="submit" disabled={pending}>{pending ? "접수 중..." : submitLabel}</Button>
      </fieldset>
    </Form>
  );
}
