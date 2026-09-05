import { useState } from "react";
import { Button, Form, InputGroup } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import { listAdminStockLevels, updateAdminStockThreshold, type StockLevelResponse } from "@/generated/api/adminCatalog";
import { adminHeaders } from "@/shared/api";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

interface Props {
  adminKey: string;
  productId: number;
  productVariantId: number | null;
  onAuthError: () => void;
}

export function StockThresholdForm(props: Props) {
  const query = useAdminQuery(props.onAuthError, {
    queryKey: ["admin", "products", props.productId, "stock-levels"],
    queryFn: () => listAdminStockLevels({ productId: props.productId }, { headers: adminHeaders(props.adminKey) }),
  });
  if (query.isLoading) return <LoadingSpinner />;
  if (query.error) return <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }} />;
  const row = query.data?.find((level) => level.productVariantId === props.productVariantId);
  return row ? <StockThresholdEditor key={`${row.productId}:${row.productVariantId}:${row.version}`} row={row} {...props} /> : null;
}

function StockThresholdEditor({ row, adminKey, onAuthError }: Props & { row: StockLevelResponse }) {
  const [minimum, setMinimum] = useState(row.minimumStock?.toString() ?? "");
  const client = useQueryClient();
  const toast = useToast();
  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => updateAdminStockThreshold({
      productId: row.productId, productVariantId: row.productVariantId,
      minimumStock: minimum.trim() === "" ? null : Number(minimum), version: row.version,
    }, { headers: adminHeaders(adminKey) }),
    onSuccess: () => {
      toast.show("최소 보유 수량을 저장했습니다.");
      void client.invalidateQueries({ queryKey: ["admin", "products"] });
    },
  });
  const valid = minimum.trim() === "" || (Number.isInteger(Number(minimum)) && Number(minimum) >= 0 && Number(minimum) <= 2147483647);
  return (
    <Form className="border rounded p-3 mb-4" onSubmit={(event) => { event.preventDefault(); if (valid) mutation.mutate(); }}>
      <Form.Group controlId="stock-minimum">
        <Form.Label>최소 보유 수량</Form.Label>
        <InputGroup>
          <Form.Control type="number" min={0} step={1} value={minimum} disabled={mutation.isPending} placeholder="미설정" onChange={(event) => setMinimum(event.target.value)} />
          <Button type="submit" disabled={!valid || mutation.isPending}>기준 저장</Button>
        </InputGroup>
        <Form.Text>이 수량 이하이면 오늘 할 일에 표시합니다. 비워서 저장하면 품절만 표시합니다.</Form.Text>
      </Form.Group>
      <ErrorAlert error={mutation.error} onRetry={() => { void client.invalidateQueries({ queryKey: ["admin", "products"] }); }} />
    </Form>
  );
}
