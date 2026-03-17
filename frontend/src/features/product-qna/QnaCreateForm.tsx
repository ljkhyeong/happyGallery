import { useState } from "react";
import { Form, Button, Card } from "react-bootstrap";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createQna } from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";

interface Props {
  productId: number;
}

export function QnaCreateForm({ productId }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [secret, setSecret] = useState(false);
  const [password, setPassword] = useState("");
  const [open, setOpen] = useState(false);

  const mutation = useMutation({
    mutationFn: () =>
      createQna(productId, {
        title,
        content,
        secret,
        password: secret ? password : undefined,
      }),
    onSuccess: () => {
      toast.show("Q&A가 등록되었습니다.");
      setTitle("");
      setContent("");
      setSecret(false);
      setPassword("");
      setOpen(false);
      queryClient.invalidateQueries({ queryKey: ["product-qna", productId] });
    },
  });

  if (!open) {
    return (
      <Button variant="outline-primary" size="sm" onClick={() => setOpen(true)}>
        질문 작성
      </Button>
    );
  }

  const canSubmit = title.trim() && content.trim() && (!secret || password.length >= 4);

  return (
    <Card className="mb-3">
      <Card.Body>
        <h6 className="mb-3">질문 작성</h6>
        <Form onSubmit={(e) => { e.preventDefault(); mutation.mutate(); }}>
          <Form.Group className="mb-2">
            <Form.Control
              placeholder="제목"
              maxLength={200}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </Form.Group>
          <Form.Group className="mb-2">
            <Form.Control
              as="textarea"
              rows={3}
              placeholder="질문 내용을 입력하세요"
              value={content}
              onChange={(e) => setContent(e.target.value)}
            />
          </Form.Group>
          <Form.Check
            type="checkbox"
            id="qna-secret"
            label="비밀글"
            checked={secret}
            onChange={(e) => setSecret(e.target.checked)}
            className="mb-2"
          />
          {secret && (
            <Form.Group className="mb-2">
              <Form.Control
                type="password"
                placeholder="비밀번호 (4자 이상)"
                minLength={4}
                maxLength={20}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <Form.Text className="text-muted">
                비밀글 확인 시 이 비밀번호를 입력해야 합니다.
              </Form.Text>
            </Form.Group>
          )}
          <ErrorAlert error={mutation.error} />
          <div className="d-flex gap-2">
            <Button type="submit" size="sm" disabled={!canSubmit || mutation.isPending}>
              {mutation.isPending ? "등록 중..." : "등록"}
            </Button>
            <Button variant="outline-secondary" size="sm" onClick={() => setOpen(false)}>
              취소
            </Button>
          </div>
        </Form>
      </Card.Body>
    </Card>
  );
}
