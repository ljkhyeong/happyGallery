import { Form } from "react-bootstrap";
import { CLASS_SITUATION_TAG_OPTIONS, type ClassSituationTagCode } from "@/shared/lib";

interface Props {
  idPrefix: string;
  value: ClassSituationTagCode[];
  onChange: (next: ClassSituationTagCode[]) => void;
}

/** 홈 상황별 바로가기와 클래스 목록 필터에 쓰는 상황 태그를 고른다. */
export function ClassSituationTagField({ idPrefix, value, onChange }: Props) {
  return (
    <Form.Group>
      <Form.Label as="span" className="d-block">상황 태그</Form.Label>
      <div className="d-flex flex-wrap gap-3">
        {CLASS_SITUATION_TAG_OPTIONS.map(({ code, label }) => (
          <Form.Check
            key={code}
            id={`${idPrefix}-${code}`}
            type="checkbox"
            label={label}
            checked={value.includes(code)}
            onChange={(event) => onChange(event.target.checked
              ? CLASS_SITUATION_TAG_OPTIONS.map((option) => option.code)
                .filter((option) => option === code || value.includes(option))
              : value.filter((tag) => tag !== code))}
          />
        ))}
      </div>
      <Form.Text muted>고객이 홈의 상황별 바로가기와 클래스 목록에서 이 수업을 찾을 수 있습니다.</Form.Text>
    </Form.Group>
  );
}
