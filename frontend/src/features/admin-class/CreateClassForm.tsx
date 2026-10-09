import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button, Col, Form, Row } from "react-bootstrap";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { ErrorAlert, useToast } from "@/shared/ui";
import { createClass } from "./api";
import { ClassCategoryField } from "./ClassCategoryField";
import { ClassDetailFields, ClassPassEligibleField } from "./ClassDetailFields";
import { createClassCategoryValue } from "./classCategories";
import {
  classCategoryChangeHandler,
  classDetailPayload,
  EMPTY_CLASS_DETAILS,
  invalidateClassQueries,
  type ClassDetailValues,
} from "./classFormRules";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function CreateClassForm({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [name, setName] = useState("");
  const [categoryValue, setCategoryValue] = useState(createClassCategoryValue);
  const [durationMin, setDurationMin] = useState("120");
  const [price, setPrice] = useState("50000");
  const [bufferMin, setBufferMin] = useState("30");
  const [capacity, setCapacity] = useState("8");
  const [passEligible, setPassEligible] = useState(true);
  const [details, setDetails] = useState<ClassDetailValues>(EMPTY_CLASS_DETAILS);

  const handleCategoryChange = classCategoryChangeHandler(setCategoryValue, setPassEligible);

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => createClass(adminKey, {
      name,
      category: categoryValue.category,
      durationMin: Number(durationMin),
      price: Number(price),
      bufferMin: Number(bufferMin),
      capacity: Number(capacity),
      passEligible,
      ...classDetailPayload(details),
    }),
    onSuccess: (bookingClass) => {
      invalidateClassQueries(queryClient);
      toast.show(`클래스 #${bookingClass.id} 생성 완료`);
      setName("");
      setCategoryValue(createClassCategoryValue());
      setDurationMin("120");
      setPrice("50000");
      setBufferMin("30");
      setCapacity("8");
      setPassEligible(true);
      setDetails(EMPTY_CLASS_DETAILS);
    },
  });

  const valid =
    name.trim().length > 0 &&
    categoryValue.category.trim().length > 0 &&
    Number(durationMin) > 0 &&
    Number(price) >= 10 &&
    Number(bufferMin) >= 0 &&
    Number(capacity) >= 1;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        if (valid) mutation.mutate();
      }}
    >
      <ErrorAlert error={mutation.error} />
      <Row className="g-3">
        <Col xs={12} md={6}>
          <Form.Group controlId="admin-class-name">
            <Form.Label>클래스명</Form.Label>
            <Form.Control
              value={name}
              maxLength={100}
              onChange={(e) => setName(e.target.value)}
              placeholder="예: 레진아트 원데이"
            />
          </Form.Group>
        </Col>
        <Col xs={12} md={6}>
          <ClassCategoryField
            controlId="admin-class-category"
            value={categoryValue}
            onChange={handleCategoryChange}
            allowEmpty
          />
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Form.Group controlId="admin-class-duration">
            <Form.Label>소요 시간(분)</Form.Label>
            <Form.Control
              type="number"
              min={1}
              value={durationMin}
              onChange={(e) => setDurationMin(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Form.Group controlId="admin-class-price">
            <Form.Label>가격 (원)</Form.Label>
            <Form.Control
              type="number"
              min={10}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Form.Group controlId="admin-class-buffer">
            <Form.Label>수업 후 정리 시간(분)</Form.Label>
            <Form.Control
              type="number"
              min={0}
              value={bufferMin}
              onChange={(e) => setBufferMin(e.target.value)}
            />
            <Form.Text muted>
              예약된 수업이 끝난 뒤 이 시간이 지나기 전에 시작하는 다음 수업은 고객이 예약할 수 없습니다.
            </Form.Text>
          </Form.Group>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Form.Group controlId="admin-class-capacity">
            <Form.Label>회차당 정원(명)</Form.Label>
            <Form.Control
              type="number"
              min={1}
              step={1}
              value={capacity}
              onChange={(e) => setCapacity(e.target.value)}
            />
            <Form.Text muted>이 클래스의 예약 회차마다 적용됩니다.</Form.Text>
          </Form.Group>
        </Col>
        <ClassDetailFields
          adminKey={adminKey}
          onAuthError={onAuthError}
          idPrefix="admin-class"
          imagePreviewAlt="등록할 클래스 대표 이미지 미리보기"
          values={details}
          onChange={(change) => setDetails((current) => ({ ...current, ...change }))}
        />
        <Col xs={12} className="d-flex flex-wrap align-items-center justify-content-between gap-3">
          <ClassPassEligibleField
            id="admin-class-pass-eligible"
            category={categoryValue.category}
            checked={passEligible}
            onChange={setPassEligible}
          />
          <Button type="submit" variant="primary" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "생성 중..." : "클래스 생성"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
