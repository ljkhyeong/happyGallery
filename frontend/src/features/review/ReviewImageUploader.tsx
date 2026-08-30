import { useEffect, useId, useMemo, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ImagePlus, Trash2, Upload } from "lucide-react";
import { Alert, Button, Form, Modal } from "react-bootstrap";
import type { MemberReviewResponse } from "@/generated/api/review";
import {
  captureCustomerSession,
  queryKeys,
  requireCurrentCustomerSession,
  runForCurrentCustomer,
  runForCustomerSession,
} from "@/shared/api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { removeReviewImage, uploadReviewImage } from "./api";
import { MemberReviewProtectedImage } from "./MemberReviewProtectedImage";
import { MAX_REVIEW_IMAGES, reviewImageSelectionError } from "./reviewUiPolicy";

export function ReviewImageUploader({ review }: { review: MemberReviewResponse }) {
  const deleteConfirmationTitleId = useId();
  const [files, setFiles] = useState<File[]>([]);
  const [pendingDeleteImage, setPendingDeleteImage] = useState<{
    imageId: number;
    index: number;
  } | null>(null);
  const [validationMessage, setValidationMessage] = useState<string | null>(null);
  const [uploadProgressMessage, setUploadProgressMessage] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();
  const toast = useToast();
  const previews = useMemo(
    () => files.map((file) => ({ file, url: URL.createObjectURL(file) })),
    [files],
  );

  useEffect(
    () => () => previews.forEach(({ url }) => URL.revokeObjectURL(url)),
    [previews],
  );

  const invalidateReviewQueries = async () => {
    const publicKey = review.targetType === "PRODUCT"
      ? queryKeys.reviews.products.byProduct(review.targetId)
      : queryKeys.reviews.classes.byClass(review.targetId);
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: publicKey }),
      queryClient.invalidateQueries({ queryKey: queryKeys.member.reviews.all }),
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.reviews.all }),
    ]);
  };

  const uploadMutation = useMutation({
    mutationFn: async (selectedFiles: File[]) => {
      const snapshot = captureCustomerSession();
      let uploadedCount = 0;
      try {
        for (const file of selectedFiles) {
          await runForCustomerSession(snapshot, () => uploadReviewImage(review.id, file));
          uploadedCount += 1;
          requireCurrentCustomerSession(snapshot);
          setFiles((current) => current.slice(1));
        }
      } catch (error) {
        requireCurrentCustomerSession(snapshot);
        setUploadProgressMessage(uploadedCount > 0
          ? `${uploadedCount}장은 등록됐고 ${selectedFiles.length - uploadedCount}장은 등록하지 못했습니다. 등록된 사진과 남은 선택을 확인한 뒤 다시 시도해 주세요.`
          : "사진을 등록하지 못했습니다. 등록된 사진과 선택 목록을 확인한 뒤 다시 시도해 주세요.");
        throw error;
      } finally {
        requireCurrentCustomerSession(snapshot);
        await invalidateReviewQueries();
        requireCurrentCustomerSession(snapshot);
      }

      requireCurrentCustomerSession(snapshot);
      setFiles([]);
      setUploadProgressMessage(null);
      if (inputRef.current) inputRef.current.value = "";
      toast.show("후기 사진을 등록했습니다.");
      requireCurrentCustomerSession(snapshot);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (imageId: number) => runForCurrentCustomer(
      () => removeReviewImage(review.id, imageId),
      async (_, requireCurrent) => {
        requireCurrent();
        await invalidateReviewQueries();
        requireCurrent();
        toast.show("후기 사진을 삭제했습니다.");
      },
    ),
    onSuccess: () => setPendingDeleteImage(null),
  });

  const remainingCount = MAX_REVIEW_IMAGES - review.images.length;

  return (
    <section className="review-image-manager" aria-label="후기 사진 관리">
      {review.images.length > 0 && (
        <div className="review-image-gallery review-image-gallery-editable">
          {review.images.map((image, index) => (
            <div key={image.id} className="review-image-edit-item">
              {review.status === "HIDDEN" ? (
                <MemberReviewProtectedImage
                  reviewId={review.id}
                  imageId={image.id}
                  alt={`등록한 후기 사진 ${index + 1}`}
                />
              ) : (
                <img src={image.imageUrl} alt={`등록한 후기 사진 ${index + 1}`} />
              )}
              <Button
                type="button"
                size="sm"
                variant="danger"
                aria-label={`등록한 후기 사진 ${index + 1} 삭제`}
                aria-haspopup="dialog"
                disabled={deleteMutation.isPending || uploadMutation.isPending}
                onClick={() => {
                  uploadMutation.reset();
                  deleteMutation.reset();
                  setPendingDeleteImage({ imageId: image.id, index });
                }}
              >
                <Trash2 size={14} aria-hidden="true" />
              </Button>
            </div>
          ))}
        </div>
      )}

      {remainingCount > 0 && (
        <div className="review-image-upload mt-3">
          <Form.Label htmlFor={`review-images-${review.id}`} className="review-image-upload-label">
            <ImagePlus size={17} aria-hidden="true" /> 사진 추가
            <span>JPEG·PNG, 장당 5MB 이하 · 최대 {MAX_REVIEW_IMAGES}장</span>
          </Form.Label>
          <Form.Control
            ref={inputRef}
            id={`review-images-${review.id}`}
            type="file"
            accept="image/jpeg,image/png"
            multiple
            disabled={uploadMutation.isPending || deleteMutation.isPending}
            aria-invalid={validationMessage !== null}
            aria-describedby={validationMessage ? `review-image-error-${review.id}` : undefined}
            onChange={(event) => {
              uploadMutation.reset();
              deleteMutation.reset();
              setUploadProgressMessage(null);
              const input = event.currentTarget as HTMLInputElement;
              const selected = Array.from(input.files ?? []);
              const error = reviewImageSelectionError(selected, remainingCount);
              if (error) {
                setValidationMessage(error);
                setFiles([]);
                input.value = "";
                return;
              }
              setValidationMessage(null);
              setFiles(selected);
            }}
          />
          {validationMessage && (
            <div id={`review-image-error-${review.id}`} className="invalid-feedback d-block">
              {validationMessage}
            </div>
          )}
        </div>
      )}

      {previews.length > 0 && (
        <div className="review-image-preview" aria-label="등록 전 사진 미리보기">
          {previews.map(({ file, url }, index) => (
            <div key={`${file.name}-${file.lastModified}-${index}`} className="review-image-preview-item">
              <img src={url} alt={`등록 전 사진 ${index + 1}`} />
              <Button
                type="button"
                size="sm"
                variant="secondary"
                aria-label={`등록 전 사진 ${index + 1} 선택에서 제거`}
                disabled={uploadMutation.isPending}
                onClick={() => {
                  setFiles((current) => current.filter((_, candidateIndex) => candidateIndex !== index));
                  setUploadProgressMessage(null);
                  if (inputRef.current) inputRef.current.value = "";
                }}
              >
                <Trash2 size={14} aria-hidden="true" />
              </Button>
            </div>
          ))}
        </div>
      )}

      {uploadProgressMessage && (
        <Alert variant="warning" className="small mt-2 mb-0" role="status">
          {uploadProgressMessage}
        </Alert>
      )}

      {files.length > 0 && (
        <div className="d-flex gap-2 mt-2">
          <Button
            type="button"
            size="sm"
            variant="dark"
            disabled={uploadMutation.isPending}
            onClick={() => {
              setUploadProgressMessage(null);
              uploadMutation.mutate(files);
            }}
          >
            <Upload size={14} aria-hidden="true" /> {uploadMutation.isPending ? "등록 중..." : `${files.length}장 등록`}
          </Button>
          <Button
            type="button"
            size="sm"
            variant="outline-secondary"
            disabled={uploadMutation.isPending}
            onClick={() => {
              uploadMutation.reset();
              setFiles([]);
              setValidationMessage(null);
              setUploadProgressMessage(null);
              if (inputRef.current) inputRef.current.value = "";
            }}
          >
            선택 취소
          </Button>
        </div>
      )}
      <div className="mt-2">
        <ErrorAlert error={uploadMutation.error} />
      </div>
      <Modal
        show={pendingDeleteImage !== null}
        onHide={() => {
          if (deleteMutation.isPending) return;
          deleteMutation.reset();
          setPendingDeleteImage(null);
        }}
        centered
        aria-labelledby={deleteConfirmationTitleId}
      >
        <Modal.Header closeButton={!deleteMutation.isPending} closeLabel="닫기">
          <Modal.Title id={deleteConfirmationTitleId} className="fs-6">
            후기 사진 삭제
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="mb-0">
            등록한 후기 사진 {(pendingDeleteImage?.index ?? 0) + 1}을 삭제할까요?
            삭제한 사진은 복구할 수 없습니다.
          </p>
          <div className="mt-3"><ErrorAlert error={deleteMutation.error} /></div>
        </Modal.Body>
        <Modal.Footer>
          <Button
            type="button"
            variant="outline-secondary"
            disabled={deleteMutation.isPending}
            onClick={() => {
              deleteMutation.reset();
              setPendingDeleteImage(null);
            }}
          >
            취소
          </Button>
          <Button
            type="button"
            variant="danger"
            disabled={deleteMutation.isPending || pendingDeleteImage === null}
            onClick={() => {
              if (pendingDeleteImage) deleteMutation.mutate(pendingDeleteImage.imageId);
            }}
          >
            {deleteMutation.isPending ? "삭제 중..." : "사진 삭제"}
          </Button>
        </Modal.Footer>
      </Modal>
    </section>
  );
}
