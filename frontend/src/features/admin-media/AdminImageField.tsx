import { useEffect, useState } from "react";
import { Form } from "react-bootstrap";
import { isAdminSessionUnauthorized } from "@/shared/hooks/adminSessionUnauthorized";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
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
  const [previewUrl, setPreviewUrl] = useState<string>();
  const [previewError, setPreviewError] = useState<unknown>();
  const upload = useAdminMutation(onAuthError, {
    mutationFn: (file: File) => uploadAdminImage(adminKey, file),
    onSuccess: (result) => onChange(result.url),
  });

  useEffect(() => {
    setPreviewError(undefined);
    if (!value) {
      setPreviewUrl(undefined);
      return;
    }
    const fileName = localAdminImageFileName(value);
    if (!fileName) {
      setPreviewUrl(value);
      return;
    }

    const controller = new AbortController();
    let objectUrl: string | undefined;
    setPreviewUrl(undefined);
    void fetchAdminImagePreview(adminKey, fileName, controller.signal)
      .then((blob) => {
        if (controller.signal.aborted) return;
        objectUrl = URL.createObjectURL(blob);
        setPreviewUrl(objectUrl);
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        if (isAdminSessionUnauthorized(error)) onAuthError();
        setPreviewError(error);
      });

    return () => {
      controller.abort();
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [adminKey, onAuthError, value]);

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
      <ErrorAlert error={previewError} />
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
