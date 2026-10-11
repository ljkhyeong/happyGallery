import { Form } from "react-bootstrap";
import { SHOP_CATEGORIES } from "@/features/product/shopCategories";

interface Props {
  controlId: string;
  value: string;
  onChange: (value: string) => void;
}

const SHOP_CATEGORY_NAMES: readonly string[] = SHOP_CATEGORIES;

/**
 * 상품 카테고리 입력. 쇼핑몰 머리글 카테고리와 같은 이름을 고르도록 목록을 붙인다.
 * 서버는 이름이 정확히 같아야 같은 카테고리로 묶으므로, 목록 밖 이름이면 메뉴로 찾을 수 없다고 알린다.
 */
export function ProductCategoryField({ controlId, value, onChange }: Props) {
  const name = value.trim();
  const outsideMenu = name !== "" && !SHOP_CATEGORY_NAMES.includes(name);

  return (
    <Form.Group controlId={controlId}>
      <Form.Label>카테고리</Form.Label>
      <Form.Control
        value={value}
        maxLength={50}
        list={`${controlId}-options`}
        onChange={(event) => onChange(event.target.value)}
        placeholder="예: 가죽"
      />
      <datalist id={`${controlId}-options`}>
        {SHOP_CATEGORIES.map((category) => <option key={category} value={category} />)}
      </datalist>
      {outsideMenu && (
        <Form.Text>
          쇼핑몰 카테고리 메뉴({SHOP_CATEGORIES.join(" · ")})에 없는 이름이라 메뉴로는 찾아갈 수 없습니다.
        </Form.Text>
      )}
    </Form.Group>
  );
}
