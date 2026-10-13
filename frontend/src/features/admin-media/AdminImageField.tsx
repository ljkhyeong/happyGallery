import { useMemo } from "react";
import { Form } from "react-bootstrap";
import { isAdminSessionUnauthorized } from "@/shared/hooks/adminSessionUnauthorized";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useBlobObjectUrl } from "@/shared/hooks/useBlobObjectUrl";
import { ErrorAlert } from "@/shared/ui";
import {
  fetchAdminImagePreview,
  localAdminImageFileName,
  uploadAdminImage,
} from "./api";

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

  // 업로드한 이미지는 공개 전에도 관리자 인증으로 받아 미리 보고, 외부 주소는 그대로 보여 준다.
  const fileName = value ? localAdminImageFileName(value) : undefined;
  const loadPreview = useMemo(
    () => fileName
      ? (signal: AbortSignal) => fetchAdminImagePreview(adminKey, fileName, signal)
      : null,
    [adminKey, fileName],
  );
  const preview = useBlobObjectUrl(loadPreview, (error) => {
    if (isAdminSessionUnauthorized(error)) onAuthError();
  });
  const previewUrl = fileName ? preview.url : value || undefined;

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
      <ErrorAlert error={preview.error} />
      {value && previewUrl && (
        <div className="admin-image-preview mt-2">
          <img src={previewUrl} alt={previewAlt} />
          <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => onChange("")}>
            이미지 제거
          </button>
        </div>
      )}
    </Form.Group>
  );
}
