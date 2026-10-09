import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button, Col, Form, Modal, Row } from "react-bootstrap";
import { updateClass } from "./api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { isPerfumeClassCategory } from "@/shared/lib";
import { ErrorAlert, useToast } from "@/shared/ui";
import type { ClassResponse } from "@/shared/types";
import { ClassCategoryField } from "./ClassCategoryField";
import { ClassDetailFields, ClassPassEligibleField } from "./ClassDetailFields";
import { createClassCategoryValue } from "./classCategories";
import {
  classCategoryChangeHandler,
  classDetailPayload,
  classDetailsFrom,
  EMPTY_CLASS_DETAILS,
  invalidateClassQueries,
  type ClassDetailValues,
} from "./classFormRules";

interface Props {
  adminKey: string;
  bookingClass: ClassResponse | null;
  onClose: () => void;
  onAuthError: () => void;
}

export function ClassEditModal({ adminKey, bookingClass, onClose, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [name, setName] = useState("");
  const [categoryValue, setCategoryValue] = useState(createClassCategoryValue);
  const [price, setPrice] = useState("");
  const [passEligible, setPassEligible] = useState(false);
  const [details, setDetails] = useState<ClassDetailValues>(EMPTY_CLASS_DETAILS);

  useEffect(() => {
    if (!bookingClass) return;
    const nextCategoryValue = createClassCategoryValue(bookingClass.category);
    setName(bookingClass.name);
    setCategoryValue(nextCategoryValue);
    setPrice(String(bookingClass.price));
    setPassEligible(
      bookingClass.passEligible && !isPerfumeClassCategory(nextCategoryValue.category),
    );
    setDetails(classDetailsFrom(bookingClass));
  }, [bookingClass]);

  const handleCategoryChange = classCategoryChangeHandler(setCategoryValue, setPassEligible);

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateClass(adminKey, bookingClass!.id, {
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
      show={bookingClass != null}
      aria-labelledby="admin-class-edit-title"
      onHide={onClose}
      centered
      size="lg"
    >
      <Form onSubmit={(event) => {
        event.preventDefault();
        if (valid) mutation.mutate();
      }}>
        <Modal.Header closeButton>
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
