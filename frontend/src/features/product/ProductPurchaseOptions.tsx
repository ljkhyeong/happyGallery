import { useMemo, useState } from "react";
import { Alert, Button, Form, Table } from "react-bootstrap";
import type {
  ProductDetailResponse,
  ProductOptionGroupResponse,
  ProductVariantResponse,
} from "@/generated/api/product";
import type { OrderTextInput } from "@/generated/api/payment";
import { formatKRW } from "@/shared/lib";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";

export interface PurchaseLine {
  key: string;
  productVariantId: number;
  textInputs: OrderTextInput[];
  label: string;
  unitPrice: number;
  availableQuantity: number;
  qty: number;
}

interface Props {
  product: ProductDetailResponse;
  lines: PurchaseLine[];
  onChange: (lines: PurchaseLine[]) => void;
}

function matchesVariant(
  variant: ProductVariantResponse,
  selectedValues: Record<string, string>,
) {
  const selected = Object.entries(selectedValues).filter(([, value]) => value.length > 0);
  return variant.selections.length === selected.length
    && selected.every(([groupKey, valueKey]) => variant.selections.some(
      (selection) => selection.groupKey === groupKey && selection.valueKey === valueKey,
    ));
}

function lineKey(variantId: number, textInputs: OrderTextInput[]) {
  return `${variantId}:${[...textInputs]
    .sort((left, right) => left.groupKey.localeCompare(right.groupKey))
    .map((input) => `${input.groupKey}=${input.value ?? ""}`)
    .join("|")}`;
}

function valueLabel(group: ProductOptionGroupResponse, valueKey: string) {
  return group.values.find((value) => value.key === valueKey)?.name ?? valueKey;
}

export function ProductPurchaseOptions({ product, lines, onChange }: Props) {
  const selectGroups = useMemo(
    () => product.optionGroups.filter((group) => group.type === "SELECT"),
    [product.optionGroups],
  );
  const textGroups = useMemo(
    () => product.optionGroups.filter((group) => group.type === "TEXT"),
    [product.optionGroups],
  );
  const [selectedValues, setSelectedValues] = useState<Record<string, string>>({});
  const [textValues, setTextValues] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);

  const selectedVariant = product.variants.find(
    (variant) => matchesVariant(variant, selectedValues),
  );
  const requiredComplete = selectGroups.every(
    (group) => !group.required || Boolean(selectedValues[group.key]),
  ) && textGroups.every(
    (group) => !group.required || Boolean(textValues[group.key]?.trim()),
  );

  const addLine = () => {
    if (!requiredComplete) {
      setMessage("필수 옵션을 모두 선택하거나 입력해 주세요.");
      return;
    }
    if (!selectedVariant) {
      setMessage("선택한 옵션 조합은 현재 판매하지 않습니다.");
      return;
    }
    if (selectedVariant.quantity < 1) {
      setMessage("선택한 옵션 조합은 품절되었습니다.");
      return;
    }
    const textInputs = textGroups
      .map((group) => ({ groupKey: group.key, value: textValues[group.key]?.trim() }))
      .filter((input) => Boolean(input.value));
    const textAdjustment = textGroups.reduce(
      (sum, group) => sum + (textValues[group.key]?.trim()
        ? (group.inputPriceAdjustment ?? 0)
        : 0),
      0,
    );
    const key = lineKey(selectedVariant.id, textInputs);
    const labels = [
      ...selectGroups.flatMap((group) => selectedValues[group.key]
        ? [`${group.name}: ${valueLabel(group, selectedValues[group.key] ?? "")}`]
        : []),
      ...textGroups.flatMap((group) => textValues[group.key]?.trim()
        ? [`${group.name}: ${textValues[group.key]?.trim()}`]
        : []),
    ];
    const existing = lines.find((line) => line.key === key);
    if (existing) {
      if (existing.qty >= Math.min(MAX_PRODUCT_QUANTITY, existing.availableQuantity)) {
        setMessage("이 옵션 조합은 더 담을 수 없습니다.");
        return;
      }
      onChange(lines.map((line) => line.key === key ? { ...line, qty: line.qty + 1 } : line));
    } else {
      onChange([...lines, {
        key,
        productVariantId: selectedVariant.id,
        textInputs,
        label: labels.join(" / ") || "기본 조합",
        unitPrice: product.price + selectedVariant.priceAdjustment + textAdjustment,
        availableQuantity: selectedVariant.quantity,
        qty: 1,
      }]);
    }
    setMessage(null);
  };

  return (
    <div className="store-option-form">
      {selectGroups.map((group) => (
        <Form.Group key={group.key} controlId={`product-option-${group.key}`}>
          <Form.Label>
            {group.name} {group.required && <span className="text-danger">*</span>}
          </Form.Label>
          <Form.Select
            value={selectedValues[group.key] ?? ""}
            onChange={(event) => {
              setSelectedValues((current) => ({ ...current, [group.key]: event.target.value }));
              setMessage(null);
            }}
          >
            <option value="">{group.required ? "선택해 주세요" : "선택 안 함"}</option>
            {group.values.map((value) => (
              <option key={value.key} value={value.key}>{value.name}</option>
            ))}
          </Form.Select>
        </Form.Group>
      ))}

      {textGroups.map((group) => (
        <Form.Group key={group.key} controlId={`product-option-${group.key}`}>
          <Form.Label>
            {group.name} {group.required && <span className="text-danger">*</span>}
            {(group.inputPriceAdjustment ?? 0) > 0 && (
              <span className="text-muted small ms-2">+{formatKRW(group.inputPriceAdjustment ?? 0)}</span>
            )}
          </Form.Label>
          <Form.Control
            value={textValues[group.key] ?? ""}
            maxLength={group.inputMaxLength ?? 200}
            placeholder={group.inputPlaceholder ?? undefined}
            onChange={(event) => {
              setTextValues((current) => ({ ...current, [group.key]: event.target.value }));
              setMessage(null);
            }}
          />
          <Form.Text>{(textValues[group.key] ?? "").length}/{group.inputMaxLength ?? 200}자</Form.Text>
        </Form.Group>
      ))}

      {message && <Alert variant="warning" className="py-2 mb-0">{message}</Alert>}
      <Button type="button" variant="outline-dark" onClick={addLine}>
        선택한 옵션 추가
      </Button>

      {lines.length > 0 && (
        <Table responsive size="sm" className="align-middle mb-0">
          <tbody>
            {lines.map((line) => (
              <tr key={line.key}>
                <td>
                  <div className="small fw-semibold">{line.label}</div>
                  <div className="small text-muted">{formatKRW(line.unitPrice)}</div>
                </td>
                <td style={{ width: 100 }}>
                  <Form.Control
                    size="sm"
                    type="number"
                    min={1}
                    max={Math.min(MAX_PRODUCT_QUANTITY, line.availableQuantity)}
                    value={line.qty}
                    onChange={(event) => {
                      const qty = Number(event.target.value);
                      if (Number.isInteger(qty)
                        && qty >= 1
                        && qty <= Math.min(MAX_PRODUCT_QUANTITY, line.availableQuantity)) {
                        onChange(lines.map((item) => item.key === line.key ? { ...item, qty } : item));
                      }
                    }}
                  />
                </td>
                <td className="text-end" style={{ width: 70 }}>
                  <Button
                    type="button"
                    size="sm"
                    variant="link"
                    className="text-danger"
                    onClick={() => onChange(lines.filter((item) => item.key !== line.key))}
                  >
                    삭제
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}
