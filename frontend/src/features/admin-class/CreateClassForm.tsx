import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button, Col, Form, Row } from "react-bootstrap";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { ErrorAlert, useToast } from "@/shared/ui";
import { createClass } from "./api";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function CreateClassForm({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [name, setName] = useState("");
  const [category, setCategory] = useState("");
  const [durationMin, setDurationMin] = useState("120");
  const [price, setPrice] = useState("50000");
  const [bufferMin, setBufferMin] = useState("30");

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => createClass(adminKey, {
      name,
      category,
      durationMin: Number(durationMin),
      price: Number(price),
      bufferMin: Number(bufferMin),
    }),
    onSuccess: (bookingClass) => {
      queryClient.invalidateQueries({ queryKey: ["classes"] });
      toast.show(`클래스 #${bookingClass.id} 생성 완료`);
      setName("");
      setCategory("");
      setDurationMin("120");
      setPrice("50000");
      setBufferMin("30");
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 401) {
        onAuthError();
      }
    },
  });

  const valid =
    name.trim().length > 0 &&
    category.trim().length > 0 &&
    Number(durationMin) > 0 &&
    Number(price) > 0 &&
    Number(bufferMin) >= 0;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        if (valid) mutation.mutate();
      }}
    >
      <ErrorAlert error={mutation.error} />
      <Row className="g-2 align-items-end">
        <Col xs={12} md={3}>
          <Form.Group controlId="admin-class-name">
            <Form.Label>클래스명</Form.Label>
            <Form.Control
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="예: 향수 원데이"
            />
          </Form.Group>
        </Col>
        <Col xs={12} sm={6} md={2}>
          <Form.Group controlId="admin-class-category">
            <Form.Label>카테고리</Form.Label>
            <Form.Control
              value={category}
              onChange={(e) => setCategory(e.target.value.toUpperCase())}
              placeholder="예: PERFUME"
            />
          </Form.Group>
        </Col>
        <Col xs={12} sm={6} md={2}>
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
        <Col xs={12} sm={6} md={2}>
          <Form.Group controlId="admin-class-price">
            <Form.Label>가격 (원)</Form.Label>
            <Form.Control
              type="number"
              min={1}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12} sm={6} md={1}>
          <Form.Group controlId="admin-class-buffer">
            <Form.Label>버퍼</Form.Label>
            <Form.Control
              type="number"
              min={0}
              value={bufferMin}
              onChange={(e) => setBufferMin(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12} md={2}>
          <Button type="submit" variant="primary" className="w-100" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "생성 중..." : "클래스 생성"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
