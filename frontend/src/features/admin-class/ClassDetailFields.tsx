import { Col, Form } from "react-bootstrap";
import { isPerfumeClassCategory } from "@/shared/lib";
import { AdminImageField } from "@/features/admin-media/AdminImageField";
import { ClassSituationTagField } from "./ClassSituationTagField";
import type { ClassDetailValues } from "./classFormRules";

interface ClassDetailFieldsProps {
  adminKey: string;
  onAuthError: () => void;
  /** 입력 칸 id 앞부분. 등록·수정 폼이 같은 화면에 있어도 겹치지 않게 다르게 준다. */
  idPrefix: string;
  imagePreviewAlt: string;
  values: ClassDetailValues;
  onChange: (change: Partial<ClassDetailValues>) => void;
}

/** 대표 이미지·상세 설명·준비물·추천 대상·상황 태그 입력. Row 안에 Col로 놓는다. */
export function ClassDetailFields({
  adminKey,
  onAuthError,
  idPrefix,
  imagePreviewAlt,
  values,
  onChange,
}: ClassDetailFieldsProps) {
  return (
    <>
      <Col xs={12}>
        <AdminImageField
          adminKey={adminKey}
          value={values.imageUrl}
          onChange={(imageUrl) => onChange({ imageUrl })}
          onAuthError={onAuthError}
          controlId={`${idPrefix}-image`}
          previewAlt={imagePreviewAlt}
        />
      </Col>
      <Col xs={12}>
        <Form.Group controlId={`${idPrefix}-description`}>
          <Form.Label>상세 설명</Form.Label>
          <Form.Control
            as="textarea"
            rows={4}
            value={values.description}
            maxLength={5000}
            onChange={(event) => onChange({ description: event.target.value })}
          />
        </Form.Group>
      </Col>
      <Col xs={12} md={6}>
        <Form.Group controlId={`${idPrefix}-preparation-info`}>
          <Form.Label>준비물 안내</Form.Label>
          <Form.Control
            as="textarea"
            rows={3}
            value={values.preparationInfo}
            maxLength={2000}
            onChange={(event) => onChange({ preparationInfo: event.target.value })}
          />
        </Form.Group>
      </Col>
      <Col xs={12} md={6}>
        <Form.Group controlId={`${idPrefix}-target-audience`}>
          <Form.Label>추천 대상</Form.Label>
          <Form.Control
            as="textarea"
            rows={3}
            value={values.targetAudience}
            maxLength={1000}
            onChange={(event) => onChange({ targetAudience: event.target.value })}
          />
        </Form.Group>
      </Col>
      <Col xs={12}>
        <ClassSituationTagField
          idPrefix={`${idPrefix}-tag`}
          value={values.situationTags}
          onChange={(situationTags) => onChange({ situationTags })}
        />
      </Col>
    </>
  );
}

interface ClassPassEligibleFieldProps {
  id: string;
  category: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}

/** 정규 이용권 사용 여부. 향수 수업이면 선택할 수 없다. */
export function ClassPassEligibleField({ id, category, checked, onChange }: ClassPassEligibleFieldProps) {
  const perfumeClass = isPerfumeClassCategory(category);
  return (
    <div>
      <Form.Check
        id={id}
        type="checkbox"
        label="정규 이용권 사용 가능"
        checked={checked}
        disabled={perfumeClass}
        onChange={(event) => onChange(event.target.checked)}
      />
      {perfumeClass && (
        <Form.Text muted>향수 수업에는 이용권을 적용할 수 없습니다.</Form.Text>
      )}
    </div>
  );
}
