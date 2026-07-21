import { Form } from "react-bootstrap";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { ErrorAlert } from "@/shared/ui";
import { uploadAdminImage } from "./api";

interface Props {
  adminKey: string;
  value: string;
  onChange: (url: string) => void;
  onAuthError: () => void;
  controlId: string;
  previewAlt: string;
}

export function AdminImageField({
  adminKey,
  value,
  onChange,
  onAuthError,
  controlId,
  previewAlt,
}: Props) {
  const upload = useAdminMutation(onAuthError, {
    mutationFn: (file: File) => uploadAdminImage(adminKey, file),
    onSuccess: (result) => onChange(result.url),
  });

  return (
    <Form.Group controlId={controlId}>
      <Form.Label>대표 이미지</Form.Label>
      <Form.Control
        type="file"
        accept="image/jpeg,image/png,image/webp"
        disabled={upload.isPending}
        onChange={(event) => {
          const file = (event.target as HTMLInputElement).files?.[0];
          if (file) upload.mutate(file);
        }}
      />
      <Form.Text>{upload.isPending ? "업로드 중..." : "JPEG, PNG, WebP · 최대 5MB"}</Form.Text>
      <ErrorAlert error={upload.error} />
      {value && (
        <div className="admin-image-preview mt-2">
          <img src={value} alt={previewAlt} />
          <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => onChange("")}>
            이미지 제거
          </button>
        </div>
      )}
    </Form.Group>
  );
}
