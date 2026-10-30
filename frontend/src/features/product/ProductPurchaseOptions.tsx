import { useMemo, useState } from "react";
import { RestockAlertButton } from "./RestockAlertButton";
import { Alert, Button, Form, Table } from "react-bootstrap";
import type {
  ProductDetailResponse,
  ProductVariantResponse,
} from "@/generated/api/product";
import type { ProductTextInputRequest } from "@/generated/api/customerStore";
import { formatKRW } from "@/shared/lib";
import { contentLengthLabel } from "@/shared/validation/contentText";
import { sumQuantitiesByVariant } from "./purchaseQuantity";
import { productQuantityLimit, remainingAddableQuantity, remainingLineQuantity } from "./purchaseStock";
import { productOptionLineKey } from "./optionLineKey";
import { productSelectionView } from "./productSelectionView";

export interface PurchaseLine {
  key: string;
  productVariantId: number;
  textInputs: ProductTextInputRequest[];
  qty: number;
}

interface Props {
  product: ProductDetailResponse;
  initialVariant?: ProductVariantResponse;
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

export function ProductPurchaseOptions({ product, initialVariant, lines, onChange }: Props) {
  const selectGroups = useMemo(
    () => product.optionGroups.filter((group) => group.type === "SELECT"),
    [product.optionGroups],
  );
  const textGroups = useMemo(
    () => product.optionGroups.filter((group) => group.type === "TEXT"),
    [product.optionGroups],
  );
  const [selectedValues, setSelectedValues] = useState<Record<string, string>>(() => Object.fromEntries(
    (initialVariant?.selections ?? []).map(({ groupKey, valueKey }) => [groupKey, valueKey]),
  ));
  const [textValues, setTextValues] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const selectedQuantities = useMemo(() => sumQuantitiesByVariant(lines), [lines]);
  const currentSelectedValues = Object.fromEntries(selectGroups.map((group) => [
    group.key, group.values.find((value) => value.key === selectedValues[group.key])?.key ?? "",
  ]));

  const selectedVariant = product.variants.find(
    (variant) => variant.active && matchesVariant(variant, currentSelectedValues),
  );
  const remainingQuantity = selectedVariant
    ? remainingAddableQuantity(
      productQuantityLimit(product, selectedVariant.id),
      selectedQuantities.get(selectedVariant.id) ?? 0,
    )
    : 0;
  // 모든 조합이 품절이라 판매할 수 없으면 담을 칸 없이 선택 옵션만 두어 그 조합의 재입고 알림을 받게 한다.
  const purchasable = product.available;
  // 다른 칸에서 고른 값과 이어지는 판매 조합이 모두 재고 0이면 목록에 품절을 붙인다. 고를 수는 있어 재입고 알림으로 이어진다.
  const isSoldOutValue = (groupKey: string, valueKey: string) => {
    const variants = product.variants.filter((variant) => variant.active
      && variant.selections.some((selection) => selection.groupKey === groupKey && selection.valueKey === valueKey)
      && selectGroups.every((other) => other.key === groupKey || !currentSelectedValues[other.key]
        || variant.selections.some((selection) => selection.groupKey === other.key
          && selection.valueKey === currentSelectedValues[other.key])));
    return variants.length > 0 && variants.every((variant) => variant.quantity === 0);
  };
  const requiredComplete = selectGroups.every(
    (group) => !group.required || Boolean(currentSelectedValues[group.key]),
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
            {group.required && "[필수] "}{group.name}
          </Form.Label>
          <Form.Select
            value={currentSelectedValues[group.key] ?? ""}
            onChange={(event) => {
              setSelectedValues((current) => ({ ...current, [group.key]: event.target.value }));
              setMessage(null);
            }}
          >
            <option value="">{group.required ? "선택해 주세요" : "선택 안 함"}</option>
            {group.values.map((value) => (
              <option key={value.key} value={value.key}>
                {isSoldOutValue(group.key, value.key) ? `${value.name} · 품절` : value.name}
              </option>
            ))}
          </Form.Select>
        </Form.Group>
      ))}

      {purchasable && textGroups.map((group) => (
        <Form.Group key={group.key} controlId={`product-option-${group.key}`}>
          <Form.Label>
            {group.required && "[필수] "}{group.name}
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
          <Form.Text>{contentLengthLabel(textValues[group.key] ?? "", group.inputMaxLength ?? 200)}</Form.Text>
        </Form.Group>
      ))}

      {purchasable && selectedVariant && (
        <Form.Text className="d-block">
          같은 옵션 조합으로 추가 가능: {remainingQuantity}개
        </Form.Text>
      )}
      {message && <Alert variant="warning" className="py-2 mb-0">{message}</Alert>}
      {selectedVariant && selectedVariant.quantity === 0 && <RestockAlertButton productId={product.id} productVariantId={selectedVariant.id} />}
      {purchasable && (
        <Button type="button" variant="outline-dark" onClick={addLine}>
          선택한 옵션 추가
        </Button>
      )}

      {purchasable && lines.length > 0 && (
        <Table responsive size="sm" className="align-middle mb-0">
          <tbody>
            {lines.map((line) => {
              const view = productSelectionView(product, line);
              const maximumLineQuantity = remainingLineQuantity(
                productQuantityLimit(product, line.productVariantId),
                selectedQuantities.get(line.productVariantId) ?? 0,
                line.qty,
              );
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
                      type="number" inputMode="numeric"
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
                      variant="outline-dark"
                      className="text-nowrap"
                      aria-label={`${view.label} 삭제`}
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
