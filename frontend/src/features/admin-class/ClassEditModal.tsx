import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button, Col, Form, Modal, Row } from "react-bootstrap";
import { updateClass } from "./api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { isPerfumeClassCategory } from "@/shared/lib";
import { ErrorAlert, useToast } from "@/shared/ui";
import type { ClassResponse } from "@/generated/api/booking";
import { ClassCategoryField } from "./ClassCategoryField";
import { ClassDetailFields, ClassPassEligibleField } from "./ClassDetailFields";
import { createClassCategoryValue } from "./classCategories";
import {
  classCategoryChangeHandler,
  classDetailPayload,
  classDetailsFrom,
  invalidateClassQueries,
} from "./classFormRules";

interface Props {
  adminKey: string;
  bookingClass: ClassResponse;
  onClose: () => void;
  onAuthError: () => void;
}

export function ClassEditModal({ adminKey, bookingClass, onClose, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  // 목록이 수정할 클래스마다 key를 바꿔 새로 만들므로 처음 값은 한 번만 채운다.
  const [name, setName] = useState(bookingClass.name);
  const [categoryValue, setCategoryValue] = useState(
    () => createClassCategoryValue(bookingClass.category),
  );
  const [price, setPrice] = useState(() => String(bookingClass.price));
  const [passEligible, setPassEligible] = useState(() => bookingClass.passEligible
    && !isPerfumeClassCategory(createClassCategoryValue(bookingClass.category).category));
  const [details, setDetails] = useState(() => classDetailsFrom(bookingClass));

  const handleCategoryChange = classCategoryChangeHandler(setCategoryValue, setPassEligible);

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateClass(adminKey, bookingClass.id, {
      name,
      category: categoryValue.category,
      price: Number(price),
      passEligible,
      ...classDetailPayload(details),
    }),
    onSuccess: () => {
      invalidateClassQueries(queryClient);
      toast.show("클래스 정보가 수정되었습니다.");
      onClose();
    },
  });

  const valid =
    name.trim().length > 0
    && categoryValue.category.trim().length > 0
    && Number(price) >= 10;

  return (
    <Modal
      show
      aria-labelledby="admin-class-edit-title"
      onHide={onClose}
      centered
      size="lg"
    >
      <Form onSubmit={(event) => {
        event.preventDefault();
        if (valid) mutation.mutate();
      }}>
        <Modal.Header closeLabel="창 닫기" closeButton>
          <Modal.Title id="admin-class-edit-title">클래스 정보 수정</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={mutation.error} />
          <Row className="g-3">
            <Col xs={12} md={8}>
              <Form.Group controlId="admin-edit-class-name">
                <Form.Label>클래스명</Form.Label>
                <Form.Control value={name} maxLength={100} onChange={(e) => setName(e.target.value)} />
              </Form.Group>
            </Col>
            <Col xs={12} md={4}>
              <Form.Group controlId="admin-edit-class-price">
                <Form.Label>가격</Form.Label>
                <Form.Control type="number" min={10} value={price} onChange={(e) => setPrice(e.target.value)} />
              </Form.Group>
            </Col>
            <Col xs={12} md={6}>
              <ClassCategoryField
                controlId="admin-edit-class-category"
                value={categoryValue}
                onChange={handleCategoryChange}
              />
            </Col>
            <Col xs={12} md={6} className="d-flex align-items-end pb-2">
              <ClassPassEligibleField
                id="admin-edit-class-pass-eligible"
                category={categoryValue.category}
                checked={passEligible}
                onChange={setPassEligible}
              />
            </Col>
            <ClassDetailFields
              adminKey={adminKey}
              onAuthError={onAuthError}
              idPrefix="admin-edit-class"
              imagePreviewAlt="수정할 클래스 대표 이미지 미리보기"
              values={details}
              onChange={(change) => setDetails((current) => ({ ...current, ...change }))}
            />
          </Row>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={onClose}>취소</Button>
          <Button type="submit" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "저장 중..." : "저장"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
