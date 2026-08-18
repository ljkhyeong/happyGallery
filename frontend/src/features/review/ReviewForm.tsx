import { useEffect, useId, useState, type FormEvent } from "react";
import { Alert, Button, Form } from "react-bootstrap";
import { ErrorAlert } from "@/shared/ui";
import {
  CONTENT_BODY_MAX_LENGTH,
  contentLengthLabel,
} from "@/shared/validation/contentText";

interface Props {
  initialRating?: number;
  initialContent?: string;
  submitLabel?: string;
  autoFocusFirstInput?: boolean;
  pending?: boolean;
  error?: unknown;
  hiddenNotice?: boolean;
  onSubmit: (value: { rating: number; content: string }) => void;
  onCancel?: () => void;
}

export function ReviewForm({
  initialRating = 5,
  initialContent = "",
  submitLabel = "후기 등록",
  autoFocusFirstInput = false,
  pending = false,
  error,
  hiddenNotice = false,
  onSubmit,
  onCancel,
}: Props) {
  const inputId = useId();
  const [rating, setRating] = useState(initialRating);
  const [content, setContent] = useState(initialContent);
  const [contentTouched, setContentTouched] = useState(false);
  const contentInvalid = contentTouched && !content.trim();

  useEffect(() => {
    setRating(initialRating);
    setContent(initialContent);
    setContentTouched(false);
  }, [initialContent, initialRating]);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setContentTouched(true);
    if (!content.trim()) return;
    onSubmit({ rating, content });
  }

  return (
    <Form onSubmit={handleSubmit} className="review-form">
      {hiddenNotice && (
        <Alert variant="warning" className="small py-2">
          비공개 상태에서 내용을 수정해도 자동으로 다시 공개되지 않습니다.
        </Alert>
      )}
      <fieldset className="mb-3" disabled={pending}>
        <legend className="form-label fs-6">별점</legend>
        <div className="review-rating-input" role="radiogroup" aria-label="별점 선택">
          {[1, 2, 3, 4, 5].map((value) => (
            <span key={value}>
              <input
                className="btn-check"
                type="radio"
                name={`${inputId}-rating`}
                id={`${inputId}-rating-${value}`}
                value={value}
                checked={rating === value}
                autoFocus={autoFocusFirstInput && value === initialRating}
                onChange={() => setRating(value)}
                aria-label={`${value}점`}
                required
              />
              <label
                className={rating >= value ? "review-star selected" : "review-star"}
                htmlFor={`${inputId}-rating-${value}`}
                aria-label={`${value}점`}
              >
                ★
              </label>
            </span>
          ))}
        </div>
      </fieldset>
      <Form.Group controlId={`${inputId}-content`} className="mb-3">
        <Form.Label>후기 내용</Form.Label>
        <Form.Control
          as="textarea"
          rows={4}
          maxLength={CONTENT_BODY_MAX_LENGTH}
          required
          value={content}
          disabled={pending}
          onChange={(event) => setContent(event.target.value)}
          onBlur={() => setContentTouched(true)}
          aria-invalid={contentInvalid}
          aria-describedby={contentInvalid
            ? `${inputId}-content-error ${inputId}-count`
            : `${inputId}-count`}
          placeholder="이용 경험을 들려주세요."
        />
        {contentInvalid && (
          <div id={`${inputId}-content-error`} className="invalid-feedback d-block">
            공백이 아닌 후기 내용을 입력해 주세요.
          </div>
        )}
        <Form.Text id={`${inputId}-count`}>
          {contentLengthLabel(content, CONTENT_BODY_MAX_LENGTH)}
        </Form.Text>
      </Form.Group>
      <ErrorAlert error={error} />
      <div className="d-flex flex-wrap gap-2">
        <Button type="submit" variant="dark" disabled={pending || !content.trim()}>
          {pending ? "저장 중..." : submitLabel}
        </Button>
        {onCancel && (
          <Button type="button" variant="outline-secondary" disabled={pending} onClick={onCancel}>
            취소
          </Button>
        )}
      </div>
    </Form>
  );
}
