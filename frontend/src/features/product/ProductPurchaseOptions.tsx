import { useMemo, useState } from "react";
import { Alert, Button, Form, Table } from "react-bootstrap";
import type {
  ProductDetailResponse,
  ProductVariantResponse,
} from "@/generated/api/product";
import type { OrderTextInput } from "@/generated/api/payment";
import { formatKRW } from "@/shared/lib";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";
import { sumQuantitiesByVariant } from "./purchaseQuantity";
import { productOptionLineKey } from "./optionLineKey";
import { productSelectionView } from "./productSelectionView";

export interface PurchaseLine {
  key: string;
  productVariantId: number;
  textInputs: OrderTextInput[];
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
  const selectedQuantities = useMemo(() => sumQuantitiesByVariant(lines), [lines]);

  const selectedVariant = product.variants.find(
    (variant) => variant.active && matchesVariant(variant, selectedValues),
  );
  const remainingQuantity = selectedVariant
    ? Math.max(0, Math.min(MAX_PRODUCT_QUANTITY, selectedVariant.quantity)
      - (selectedQuantities.get(selectedVariant.id) ?? 0))
    : 0;
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
    if (remainingQuantity < 1) {
      setMessage(selectedVariant.quantity < 1
        ? "선택한 옵션 조합은 품절되었습니다."
        : "이 옵션 조합은 더 담을 수 없습니다.");
      return;
    }
    const textInputs = textGroups
      .map((group) => ({ groupKey: group.key, value: textValues[group.key]?.trim() }))
      .filter((input) => Boolean(input.value));
    const key = productOptionLineKey(product.id, selectedVariant.id, textInputs);
    const existing = lines.find((line) => line.key === key);
    if (existing) {
      onChange(lines.map((line) => line.key === key ? { ...line, qty: line.qty + 1 } : line));
    } else {
      onChange([...lines, {
        key,
        productVariantId: selectedVariant.id,
        textInputs,
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

      {selectedVariant && (
        <Form.Text className="d-block">
          같은 옵션 조합으로 추가 가능: {remainingQuantity}개
        </Form.Text>
      )}
      {message && <Alert variant="warning" className="py-2 mb-0">{message}</Alert>}
      <Button type="button" variant="outline-dark" onClick={addLine}>
        선택한 옵션 추가
      </Button>

      {lines.length > 0 && (
        <Table responsive size="sm" className="align-middle mb-0">
          <tbody>
            {lines.map((line) => {
              const view = productSelectionView(product, line);
              const variantQuantity = product.variants.find((variant) => variant.id === line.productVariantId)?.quantity ?? 0;
              const maximumLineQuantity = Math.max(0, Math.min(MAX_PRODUCT_QUANTITY, variantQuantity)
                - (selectedQuantities.get(line.productVariantId) ?? 0) + line.qty);
              return (
                <tr key={line.key}>
                  <td>
                    <div className="small fw-semibold">{view.label}</div>
                    <div className="small text-muted">{formatKRW(view.unitPrice)}</div>
                    {!view.configurationValid && (
                      <div className="small text-danger">선택한 옵션이 변경되었습니다. 이 항목을 삭제한 뒤 다시 선택해 주세요.</div>
                    )}
                  </td>
                  <td style={{ width: 100 }}>
                    <Form.Control
                      size="sm"
                      type="number"
                      min={1}
                      max={maximumLineQuantity}
                      aria-label={`${view.label} 수량`}
                      value={line.qty}
                      onChange={(event) => {
                        const qty = Number(event.target.value);
                        if (Number.isInteger(qty)
                          && qty >= 1
                          && (qty < line.qty || qty <= maximumLineQuantity)) {
                          onChange(lines.map((item) => item.key === line.key ? { ...item, qty } : item));
                          setMessage(null);
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
                      onClick={() => {
                        onChange(lines.filter((item) => item.key !== line.key));
                        setMessage(null);
                      }}
                    >
                      삭제
                    </Button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </Table>
      )}
    </div>
  );
}
