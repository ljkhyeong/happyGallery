import { Form } from "react-bootstrap";
import { CLASS_CATEGORY_OPTIONS } from "@/shared/lib";
import {
  CUSTOM_CLASS_CATEGORY,
  type ClassCategorySelection,
  type ClassCategoryValue,
  enterCustomClassCategory,
  selectClassCategory,
} from "./classCategories";

interface Props {
  controlId: string;
  value: ClassCategoryValue;
  onChange: (value: ClassCategoryValue) => void;
  allowEmpty?: boolean;
}

export function ClassCategoryField({ controlId, value, onChange, allowEmpty = false }: Props) {
  const customControlId = `${controlId}-custom`;

  return (
    <Form.Group controlId={controlId}>
      <Form.Label>수업 종류</Form.Label>
      <Form.Select
        value={value.selection}
        onChange={(event) => onChange(
          selectClassCategory(event.target.value as ClassCategorySelection),
        )}
      >
        <option value="" disabled={!allowEmpty}>수업 종류를 선택하세요</option>
        {CLASS_CATEGORY_OPTIONS.map(({ code, label }) => (
          <option key={code} value={code}>{label}</option>
        ))}
        <option value={CUSTOM_CLASS_CATEGORY}>기타 직접 입력</option>
      </Form.Select>
      {value.selection === CUSTOM_CLASS_CATEGORY && (
        <>
          <Form.Label htmlFor={customControlId} className="mt-2">
            새 수업 종류 이름
          </Form.Label>
          <Form.Control
            id={customControlId}
            value={value.category}
            maxLength={30}
            onChange={(event) => onChange(enterCustomClassCategory(event.target.value))}
            placeholder="예: 도자기"
          />
        </>
      )}
      <Form.Text muted>
        목록에 없는 수업만 기타를 선택해 이해하기 쉬운 이름을 입력하세요.
      </Form.Text>
    </Form.Group>
  );
}
