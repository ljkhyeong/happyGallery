import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button, Col, Form, Modal, Row } from "react-bootstrap";
import { updateClass } from "./api";
import { queryKeys } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { ErrorAlert, useToast } from "@/shared/ui";
import type { ClassResponse } from "@/shared/types";
import { AdminImageField } from "@/features/admin-media/AdminImageField";

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
  const [category, setCategory] = useState("");
  const [price, setPrice] = useState("");
  const [passEligible, setPassEligible] = useState(false);
  const [description, setDescription] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [preparationInfo, setPreparationInfo] = useState("");
  const [targetAudience, setTargetAudience] = useState("");

  useEffect(() => {
    if (!bookingClass) return;
    setName(bookingClass.name);
    setCategory(bookingClass.category);
    setPrice(String(bookingClass.price));
    setPassEligible(bookingClass.passEligible);
    setDescription(bookingClass.description ?? "");
    setImageUrl(bookingClass.imageUrl ?? "");
    setPreparationInfo(bookingClass.preparationInfo ?? "");
    setTargetAudience(bookingClass.targetAudience ?? "");
  }, [bookingClass]);

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateClass(adminKey, bookingClass!.id, {
      name,
      category,
      price: Number(price),
      passEligible,
      description: description.trim() || undefined,
      imageUrl: imageUrl.trim() || undefined,
      preparationInfo: preparationInfo.trim() || undefined,
      targetAudience: targetAudience.trim() || undefined,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.classes });
      queryClient.invalidateQueries({ queryKey: queryKeys.catalog.classes });
      toast.show("클래스 정보가 수정되었습니다.");
      onClose();
    },
  });

  const valid = name.trim().length > 0 && category.trim().length > 0 && Number(price) >= 10;

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
              <Form.Group controlId="admin-edit-class-category">
                <Form.Label>카테고리</Form.Label>
                <Form.Control
                  value={category}
                  maxLength={30}
                  onChange={(e) => setCategory(e.target.value.toUpperCase())}
                />
              </Form.Group>
            </Col>
            <Col xs={12} md={6} className="d-flex align-items-end pb-2">
              <Form.Check
                id="admin-edit-class-pass-eligible"
                type="checkbox"
                label="정규 8회권 사용 가능"
                checked={passEligible}
                onChange={(e) => setPassEligible(e.target.checked)}
              />
            </Col>
            <Col xs={12}>
              <AdminImageField
                adminKey={adminKey}
                value={imageUrl}
                onChange={setImageUrl}
                onAuthError={onAuthError}
                controlId="admin-edit-class-image"
                previewAlt="수정할 클래스 대표 이미지 미리보기"
              />
            </Col>
            <Col xs={12}>
              <Form.Group controlId="admin-edit-class-description">
                <Form.Label>상세 설명</Form.Label>
                <Form.Control as="textarea" rows={4} value={description} maxLength={5000} onChange={(e) => setDescription(e.target.value)} />
              </Form.Group>
            </Col>
            <Col xs={12} md={6}>
              <Form.Group controlId="admin-edit-class-preparation">
                <Form.Label>준비물 안내</Form.Label>
                <Form.Control as="textarea" rows={3} value={preparationInfo} maxLength={2000} onChange={(e) => setPreparationInfo(e.target.value)} />
              </Form.Group>
            </Col>
            <Col xs={12} md={6}>
              <Form.Group controlId="admin-edit-class-audience">
                <Form.Label>대상 안내</Form.Label>
                <Form.Control as="textarea" rows={3} value={targetAudience} maxLength={1000} onChange={(e) => setTargetAudience(e.target.value)} />
              </Form.Group>
            </Col>
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
