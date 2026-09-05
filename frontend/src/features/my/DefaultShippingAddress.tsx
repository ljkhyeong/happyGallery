import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Card, Form } from "react-bootstrap";
import { getMyDefaultShippingAddress, saveMyDefaultShippingAddress, deleteMyDefaultShippingAddress,
  type DefaultShippingAddressResponse } from "@/generated/api/customerStore";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import type { ShippingAddress } from "@/features/payment";
import { ShippingAddressFields } from "@/features/order/ShippingAddressFields";
import { isFulfillmentComplete } from "@/features/order/fulfillmentSelection";
import { runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

const addressKey = ["me", "default-shipping-address"] as const;
function useDefaultAddress() {
  const { isAuthenticated } = useCustomerAuth();
  const client = useQueryClient();
  const toast = useToast();
  const query = useQuery({ queryKey: addressKey, enabled: isAuthenticated,
    queryFn: ({ signal }) => runForCurrentCustomer(() => getMyDefaultShippingAddress({ signal })) });
  const mutation = useMutation({
    mutationFn: (address: ShippingAddress | null) => runForCurrentCustomer(async () => {
      if (!query.data) throw new Error("기본 배송지를 먼저 확인해 주세요.");
      return address ? saveMyDefaultShippingAddress({ version: query.data.version, shippingAddress: address })
        : deleteMyDefaultShippingAddress({ version: query.data.version });
    }, async (_, requireCurrent) => {
      await client.invalidateQueries({ queryKey: addressKey });
      requireCurrent();
      toast.show("기본 배송지를 변경했습니다.");
    }),
  });
  return { query, mutation, isAuthenticated };
}

export function SavedShippingAddressActions({ address, onLoad }: { address: ShippingAddress; onLoad: (address: ShippingAddress) => void }) {
  const { query, mutation, isAuthenticated } = useDefaultAddress();
  if (!isAuthenticated) return null;
  return <div className="mb-3">
    <div className="d-flex flex-wrap gap-2">
      <Button size="sm" variant="outline-secondary" disabled={!query.data?.shippingAddress || mutation.isPending}
        onClick={() => { if (query.data?.shippingAddress) onLoad(query.data.shippingAddress); }}>기본 배송지 불러오기</Button>
      <Button size="sm" variant="outline-secondary" disabled={!query.data || mutation.isPending
        || !isFulfillmentComplete({ fulfillmentType: "SHIPPING", shippingAddress: address })}
        onClick={() => mutation.mutate(address)}>이 주소를 기본 배송지로 저장</Button>
    </div>
    {query.data && !query.data.shippingAddress && <p className="small text-muted mt-1 mb-0">저장된 기본 배송지가 없습니다.</p>}
    <ErrorAlert error={query.error ?? mutation.error} onRetry={() => { void query.refetch(); }} />
  </div>;
}

export function MyDefaultShippingAddressSection() {
  const { query, mutation } = useDefaultAddress();
  const { user } = useCustomerAuth();
  return <Card id="my-default-shipping-address" className="mb-4"><Card.Body>
    <h6>기본 배송지</h6>
    <p className="small text-muted">다음 주문에서 불러올 주소입니다. 이미 접수한 주문의 배송지는 변경되지 않습니다.</p>
    {query.isLoading && <LoadingSpinner />}
    <ErrorAlert error={query.error ?? mutation.error} onRetry={() => { void query.refetch(); }} />
    {query.data && <AddressForm key={query.data.version} initial={query.data}
      defaultName={user?.name ?? ""} defaultPhone={user?.phone ?? ""} pending={mutation.isPending}
      onSave={(address) => mutation.mutate(address)} onDelete={() => mutation.mutate(null)} />}
  </Card.Body></Card>;
}

function AddressForm({ initial, defaultName, defaultPhone, pending, onSave, onDelete }: {
  initial: DefaultShippingAddressResponse; defaultName: string; defaultPhone: string; pending: boolean;
  onSave: (address: ShippingAddress) => void; onDelete: () => void;
}) {
  const [address, setAddress] = useState<ShippingAddress>(() => initial.shippingAddress ?? {
    recipientName: defaultName, phone: defaultPhone, postalCode: "", addressLine1: "", addressLine2: null,
  });
  return <Form onSubmit={(event) => { event.preventDefault(); onSave(address); }}>
    <ShippingAddressFields value={address} onChange={setAddress} />
    <div className="d-flex gap-2">
      <Button type="submit" disabled={pending || !isFulfillmentComplete({ fulfillmentType: "SHIPPING", shippingAddress: address })}>
        {pending ? "저장 중..." : "기본 배송지 저장"}</Button>
      {initial.shippingAddress && <Button variant="outline-secondary" disabled={pending} onClick={onDelete}>기본 배송지 삭제</Button>}
    </div>
  </Form>;
}
