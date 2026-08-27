import { Badge, Button, Card, Col, Form, Row, Table } from "react-bootstrap";
import type {
  ProductOptionGroupRequest,
  ProductOptionGroupResponse,
  ProductVariantRequest,
  ProductVariantResponse,
} from "@/generated/api/adminCatalog";

export type OptionGroupDraft = ProductOptionGroupRequest;
export type VariantDraft = ProductVariantRequest;

interface Props {
  groups: OptionGroupDraft[];
  variants: VariantDraft[];
  onChange: (groups: OptionGroupDraft[], variants: VariantDraft[]) => void;
}

function createKey(prefix: string) {
  return `${prefix}_${crypto.randomUUID().replaceAll("-", "")}`;
}

function variantKey(variant: Pick<ProductVariantRequest, "selections">) {
  return variant.selections
    .map((selection) => `${selection.groupKey}=${selection.valueKey}`)
    .sort()
    .join("|");
}

function generatedVariants(
  groups: OptionGroupDraft[],
  previous: VariantDraft[],
): VariantDraft[] {
  const selectGroups = groups.filter((group) => group.type === "SELECT");
  if (selectGroups.length === 0) return [];

  let combinations: ProductVariantRequest["selections"][] = [[]];
  for (const group of selectGroups) {
    const choices = [
      ...(group.required ? [] : [null]),
      ...group.values,
    ];
    combinations = combinations.flatMap((combination) => choices.map((choice) => (
      choice === null
        ? combination
        : [...combination, { groupKey: group.key, valueKey: choice.key }]
    )));
  }

  const previousByKey = new Map(previous.map((variant) => [variantKey(variant), variant]));
  return combinations.map((selections) => previousByKey.get(variantKey({ selections })) ?? ({
    selections,
    priceAdjustment: 0,
    quantity: 0,
    active: true,
  }));
}

export function optionDraftsFromProduct(product: {
  optionGroups: ProductOptionGroupResponse[];
  variants: ProductVariantResponse[];
}): { groups: OptionGroupDraft[]; variants: VariantDraft[] } {
  return {
    groups: product.optionGroups.map((group) => ({
      key: group.key,
      type: group.type,
      name: group.name,
      required: group.required,
      sortOrder: group.sortOrder,
      inputPlaceholder: group.inputPlaceholder ?? undefined,
      inputMaxLength: group.inputMaxLength ?? undefined,
      inputPriceAdjustment: group.inputPriceAdjustment ?? undefined,
      values: group.values.map((value) => ({ ...value })),
    })),
    variants: product.variants.map((variant) => ({
      selections: variant.selections.map((selection) => ({ ...selection })),
      priceAdjustment: variant.priceAdjustment,
      quantity: variant.quantity,
      active: variant.active,
    })),
  };
}

export function ProductOptionEditor({ groups, variants, onChange }: Props) {
  const selectCount = groups.filter((group) => group.type === "SELECT").length;
  const textCount = groups.filter((group) => group.type === "TEXT").length;

  const updateGroups = (nextGroups: OptionGroupDraft[]) => {
    const normalized = nextGroups.map((group, index) => ({ ...group, sortOrder: index }));
    onChange(normalized, generatedVariants(normalized, variants));
  };

  const updateGroup = (index: number, next: OptionGroupDraft) => {
    updateGroups(groups.map((group, groupIndex) => groupIndex === index ? next : group));
  };

  const addGroup = (type: "SELECT" | "TEXT") => {
    const group: OptionGroupDraft = {
      key: createKey("group"),
      type,
      name: "",
      required: true,
      sortOrder: groups.length,
      values: type === "SELECT"
        ? [{ key: createKey("value"), name: "", sortOrder: 0 }]
        : [],
      ...(type === "TEXT" ? {
        inputMaxLength: 50,
        inputPriceAdjustment: 0,
      } : {}),
    };
    updateGroups([...groups, group]);
  };

  const selectionLabel = (variant: VariantDraft) => {
    if (variant.selections.length === 0) return "선택 안 함";
    const values = new Map(groups.flatMap((group) => group.values.map((value) => [
      `${group.key}:${value.key}`,
      `${group.name || "옵션"}: ${value.name || "값"}`,
    ] as const)));
    return variant.selections
      .map((selection) => values.get(`${selection.groupKey}:${selection.valueKey}`))
      .filter(Boolean)
      .join(" / ");
  };

  return (
    <Card className="border-0 bg-body-tertiary">
      <Card.Body>
        <div className="d-flex flex-wrap justify-content-between gap-2 mb-3">
          <div>
            <h3 className="h6 mb-1">주문 옵션</h3>
            <p className="small text-muted mb-0">
              선택형은 조합별 가격·재고를 만들고, 직접입력형은 각인 문구처럼 주문마다 값을 받습니다.
            </p>
          </div>
          <div className="d-flex gap-2">
            <Button
              type="button"
              size="sm"
              variant="outline-primary"
              disabled={selectCount >= 3}
              onClick={() => addGroup("SELECT")}
            >
              선택형 추가 ({selectCount}/3)
            </Button>
            <Button
              type="button"
              size="sm"
              variant="outline-primary"
              disabled={textCount >= 5}
              onClick={() => addGroup("TEXT")}
            >
              직접입력형 추가 ({textCount}/5)
            </Button>
          </div>
        </div>

        {groups.length === 0 && (
          <p className="small text-muted mb-0">옵션이 없으면 기본 조합 하나로 재고를 관리합니다.</p>
        )}

        <div className="d-grid gap-3">
          {groups.map((group, index) => (
            <Card key={group.key}>
              <Card.Body>
                <div className="d-flex justify-content-between align-items-center mb-3">
                  <Badge bg={group.type === "SELECT" ? "primary" : "secondary"}>
                    {group.type === "SELECT" ? "선택형" : "직접입력형"}
                  </Badge>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline-danger"
                    onClick={() => updateGroups(groups.filter((_, groupIndex) => groupIndex !== index))}
                  >
                    삭제
                  </Button>
                </div>
                <Row className="g-2">
                  <Col xs={12} md={8}>
                    <Form.Control
                      aria-label={`${index + 1}번째 옵션명`}
                      value={group.name}
                      maxLength={25}
                      placeholder={group.type === "SELECT" ? "예: 색상" : "예: 각인 문구"}
                      onChange={(event) => updateGroup(index, { ...group, name: event.target.value })}
                    />
                  </Col>
                  <Col xs={12} md={4}>
                    <Form.Check
                      type="switch"
                      label="필수 옵션"
                      checked={group.required ?? false}
                      onChange={(event) => updateGroup(index, {
                        ...group,
                        required: event.target.checked,
                      })}
                    />
                  </Col>
                </Row>

                {group.type === "SELECT" ? (
                  <div className="mt-3 d-grid gap-2">
                    {group.values.map((value, valueIndex) => (
                      <div key={value.key} className="d-flex gap-2">
                        <Form.Control
                          value={value.name}
                          maxLength={25}
                          placeholder={`옵션값 ${valueIndex + 1}`}
                          onChange={(event) => updateGroup(index, {
                            ...group,
                            values: group.values.map((item, itemIndex) => itemIndex === valueIndex
                              ? { ...item, name: event.target.value }
                              : item),
                          })}
                        />
                        <Button
                          type="button"
                          variant="outline-danger"
                          disabled={group.values.length === 1}
                          onClick={() => updateGroup(index, {
                            ...group,
                            values: group.values
                              .filter((_, itemIndex) => itemIndex !== valueIndex)
                              .map((item, itemIndex) => ({ ...item, sortOrder: itemIndex })),
                          })}
                        >
                          삭제
                        </Button>
                      </div>
                    ))}
                    <Button
                      type="button"
                      size="sm"
                      variant="outline-secondary"
                      onClick={() => updateGroup(index, {
                        ...group,
                        values: [...group.values, {
                          key: createKey("value"),
                          name: "",
                          sortOrder: group.values.length,
                        }],
                      })}
                    >
                      옵션값 추가
                    </Button>
                  </div>
                ) : (
                  <Row className="g-2 mt-1">
                    <Col xs={12} md={6}>
                      <Form.Control
                        value={group.inputPlaceholder ?? ""}
                        maxLength={100}
                        placeholder="입력 안내 문구"
                        onChange={(event) => updateGroup(index, {
                          ...group,
                          inputPlaceholder: event.target.value || undefined,
                        })}
                      />
                    </Col>
                    <Col xs={6} md={3}>
                      <Form.Control
                        type="number"
                        min={1}
                        max={200}
                        aria-label="최대 입력 글자 수"
                        value={group.inputMaxLength ?? 50}
                        onChange={(event) => updateGroup(index, {
                          ...group,
                          inputMaxLength: Number(event.target.value),
                        })}
                      />
                    </Col>
                    <Col xs={6} md={3}>
                      <Form.Control
                        type="number"
                        min={0}
                        aria-label="직접입력 옵션 추가금"
                        value={group.inputPriceAdjustment ?? 0}
                        onChange={(event) => updateGroup(index, {
                          ...group,
                          inputPriceAdjustment: Number(event.target.value),
                        })}
                      />
                    </Col>
                  </Row>
                )}
              </Card.Body>
            </Card>
          ))}
        </div>

        {selectCount > 0 && (
          <div className="mt-4">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <h3 className="h6 mb-0">옵션 조합별 가격·재고</h3>
              <Badge bg={variants.length > 500 ? "danger" : "dark"}>{variants.length}/500</Badge>
            </div>
            <div className="table-responsive">
              <Table size="sm" bordered className="align-middle mb-0">
                <thead>
                  <tr>
                    <th>조합</th>
                    <th style={{ minWidth: 120 }}>추가금</th>
                    <th style={{ minWidth: 110 }}>재고</th>
                    <th>판매</th>
                  </tr>
                </thead>
                <tbody>
                  {variants.map((variant, index) => (
                    <tr key={variantKey(variant) || `empty-${index}`}>
                      <td>{selectionLabel(variant) || "선택 안 함"}</td>
                      <td>
                        <Form.Control
                          size="sm"
                          type="number"
                          value={variant.priceAdjustment ?? 0}
                          onChange={(event) => onChange(groups, variants.map((item, itemIndex) => (
                            itemIndex === index
                              ? { ...item, priceAdjustment: Number(event.target.value) }
                              : item
                          )))}
                        />
                      </td>
                      <td>
                        <Form.Control
                          size="sm"
                          type="number"
                          min={0}
                          value={variant.quantity ?? 0}
                          onChange={(event) => onChange(groups, variants.map((item, itemIndex) => (
                            itemIndex === index
                              ? { ...item, quantity: Number(event.target.value) }
                              : item
                          )))}
                        />
                      </td>
                      <td>
                        <Form.Check
                          type="switch"
                          aria-label={`${selectionLabel(variant)} 판매 여부`}
                          checked={variant.active ?? true}
                          onChange={(event) => onChange(groups, variants.map((item, itemIndex) => (
                            itemIndex === index
                              ? { ...item, active: event.target.checked }
                              : item
                          )))}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </div>
          </div>
        )}
      </Card.Body>
    </Card>
  );
}
