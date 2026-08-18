import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button, Col, Form, Row } from "react-bootstrap";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { queryKeys } from "@/shared/api";
import { isPerfumeClassCategory } from "@/shared/lib";
import { ErrorAlert, useToast } from "@/shared/ui";
import { createClass } from "./api";
import { AdminImageField } from "@/features/admin-media/AdminImageField";
import { ClassCategoryField } from "./ClassCategoryField";
import { type ClassCategoryValue, createClassCategoryValue } from "./classCategories";

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
  const [passEligible, setPassEligible] = useState(true);
  const [description, setDescription] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [preparationInfo, setPreparationInfo] = useState("");
  const [targetAudience, setTargetAudience] = useState("");

  const handleCategoryChange = (nextCategoryValue: ClassCategoryValue) => {
    setCategoryValue(nextCategoryValue);
    if (isPerfumeClassCategory(nextCategoryValue.category)) {
      setPassEligible(false);
    }
  };

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => createClass(adminKey, {
      name,
      category: categoryValue.category,
      durationMin: Number(durationMin),
      price: Number(price),
      bufferMin: Number(bufferMin),
      passEligible,
      description: description.trim() || undefined,
      imageUrl: imageUrl.trim() || undefined,
      preparationInfo: preparationInfo.trim() || undefined,
      targetAudience: targetAudience.trim() || undefined,
    }),
    onSuccess: (bookingClass) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.classes });
      queryClient.invalidateQueries({ queryKey: queryKeys.catalog.classes });
      toast.show(`클래스 #${bookingClass.id} 생성 완료`);
      setName("");
      setCategoryValue(createClassCategoryValue());
      setDurationMin("120");
      setPrice("50000");
      setBufferMin("30");
      setPassEligible(true);
      setDescription("");
      setImageUrl("");
      setPreparationInfo("");
      setTargetAudience("");
    },
  });

  const valid =
    name.trim().length > 0 &&
    categoryValue.category.trim().length > 0 &&
    Number(durationMin) > 0 &&
    Number(price) >= 10 &&
    Number(bufferMin) >= 0;

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
        <Col xs={12} sm={4}>
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
        <Col xs={12} sm={4}>
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
        <Col xs={12} sm={4}>
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
        <Col xs={12}>
          <AdminImageField
            adminKey={adminKey}
            value={imageUrl}
            onChange={setImageUrl}
            onAuthError={onAuthError}
            controlId="admin-class-image"
            previewAlt="등록할 클래스 대표 이미지 미리보기"
          />
        </Col>
        <Col xs={12}>
          <Form.Group controlId="admin-class-description">
            <Form.Label>상세 설명</Form.Label>
            <Form.Control
              as="textarea"
              rows={4}
              value={description}
              maxLength={5000}
              onChange={(e) => setDescription(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12} md={6}>
          <Form.Group controlId="admin-class-preparation-info">
            <Form.Label>준비물 안내</Form.Label>
            <Form.Control
              as="textarea"
              rows={3}
              value={preparationInfo}
              maxLength={2000}
              onChange={(e) => setPreparationInfo(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12} md={6}>
          <Form.Group controlId="admin-class-target-audience">
            <Form.Label>대상 안내</Form.Label>
            <Form.Control
              as="textarea"
              rows={3}
              value={targetAudience}
              maxLength={1000}
              onChange={(e) => setTargetAudience(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12} className="d-flex flex-wrap align-items-center justify-content-between gap-3">
          <div>
            <Form.Check
              id="admin-class-pass-eligible"
              type="checkbox"
              label="정규 8회권 사용 가능"
              checked={passEligible}
              disabled={isPerfumeClassCategory(categoryValue.category)}
              onChange={(e) => setPassEligible(e.target.checked)}
            />
            {isPerfumeClassCategory(categoryValue.category) && (
              <Form.Text muted>향수 수업에는 8회권을 적용할 수 없습니다.</Form.Text>
            )}
          </div>
          <Button type="submit" variant="primary" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "생성 중..." : "클래스 생성"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
