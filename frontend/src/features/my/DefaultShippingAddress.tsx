import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Card, Form } from "react-bootstrap";
import {
  getMyDefaultShippingAddress, saveMyDefaultShippingAddress, deleteMyDefaultShippingAddress,
} from "@/generated/api/customerStore";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import type { ShippingAddress } from "@/features/payment";
import { ShippingAddressFields } from "@/features/order/ShippingAddressFields";
import { isFulfillmentComplete } from "@/features/order/fulfillmentSelection";
import {
  ApiError, captureCustomerSession, requireCurrentCustomerSession, runForCurrentCustomer,
  type CustomerSessionSnapshot,
} from "@/shared/api";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

const addressKey = ["me", "default-shipping-address"] as const;
interface AddressChange {
  version: number;
  shippingAddress: ShippingAddress | null;
  customerSession: CustomerSessionSnapshot;
}

function useDefaultAddress() {
  const { isAuthenticated } = useCustomerAuth();
  const client = useQueryClient();
  const toast = useToast();
  const query = useQuery({
    queryKey: addressKey, enabled: isAuthenticated,
    queryFn: ({ signal }) => runForCurrentCustomer(() => getMyDefaultShippingAddress({ signal })),
  });
  const mutation = useMutation({
    mutationFn: (change: AddressChange) => runForCurrentCustomer(() => {
      requireCurrentCustomerSession(change.customerSession);
      return change.shippingAddress
        ? saveMyDefaultShippingAddress({ version: change.version, shippingAddress: change.shippingAddress })
        : deleteMyDefaultShippingAddress({ version: change.version });
    }, async (_, requireCurrent) => {
      await client.invalidateQueries({ queryKey: addressKey });
      requireCurrent();
      toast.show(change.shippingAddress ? "기본 배송지를 저장했습니다." : "기본 배송지를 삭제했습니다.");
    }),
  });
  const change = (shippingAddress: ShippingAddress | null, version: number) => {
    if (mutation.isPending) return;
    mutation.mutate({ shippingAddress, version, customerSession: captureCustomerSession() });
  };
  const retry = () => {
    if (mutation.variables) change(mutation.variables.shippingAddress, mutation.variables.version);
  };
  const conflict = mutation.error instanceof ApiError && mutation.error.code === "CONFLICT";
  return { query, mutation, change, retry, conflict, isAuthenticated };
}

export function SavedShippingAddressActions(props: { address: ShippingAddress; onLoad: (address: ShippingAddress) => void }) {
  const { sessionVersion } = useCustomerAuth();
  return <SavedAddressActions key={sessionVersion} {...props} />;
}

function SavedAddressActions({ address, onLoad }: { address: ShippingAddress; onLoad: (address: ShippingAddress) => void }) {
  const { query, mutation, change, conflict, isAuthenticated } = useDefaultAddress();
  if (!isAuthenticated) return null;
  const loadLatest = () => {
    void runForCurrentCustomer(
      () => query.refetch({ throwOnError: true }),
      ({ data }) => {
        if (data?.shippingAddress) onLoad(data.shippingAddress);
        mutation.reset();
      },
    ).catch(() => { /* 조회 오류는 아래에서 표시한다. */ });
  };
  const save = () => {
    if (query.data) change(address, mutation.isError ? mutation.variables!.version : query.data.version);
  };
  const valid = isFulfillmentComplete({ fulfillmentType: "SHIPPING", shippingAddress: address });
  return <div className="mb-3">
    <div className="d-flex flex-wrap gap-2">
      <Button size="sm" variant="outline-secondary" disabled={!query.data?.shippingAddress || mutation.isPending || query.isFetching}
        onClick={loadLatest}>기본 배송지 불러오기</Button>
      <Button size="sm" variant="outline-secondary" disabled={!query.data || query.isError || mutation.isPending || conflict || !valid}
        onClick={save}>{mutation.isPending ? "저장 중..." : "이 주소를 기본 배송지로 저장"}</Button>
    </div>
    {query.data && !query.data.shippingAddress && <p className="small text-muted mt-1 mb-0">저장된 기본 배송지가 없습니다.</p>}
    <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }} retrying={query.isFetching} />
    {conflict ? <Alert variant="warning" className="mt-2">
      기본 배송지가 다른 화면에서 변경되었습니다. 최신 주소를 불러와 확인해 주세요.
      <div className="mt-2"><Button size="sm" variant="outline-secondary" disabled={query.isFetching} onClick={loadLatest}>최신 주소 불러오기</Button></div>
    </Alert> : <ErrorAlert error={mutation.error} onRetry={valid ? save : undefined} retrying={mutation.isPending} />}
  </div>;
}

export function MyDefaultShippingAddressSection() {
  const [editedAddress, setDraft] = useState<{ version: number; shippingAddress: ShippingAddress } | null>(null);
  const { query, mutation, change, retry, conflict } = useDefaultAddress();
  const { user } = useCustomerAuth();
  const savedAndReloaded = mutation.isSuccess && query.data !== undefined && query.data.version > mutation.variables.version;
  if (savedAndReloaded && editedAddress !== null) setDraft(null);
  const draft = savedAndReloaded ? null : editedAddress;
  const address = draft?.shippingAddress ?? query.data?.shippingAddress ?? {
    recipientName: user?.name ?? "", phone: user?.phone ?? "", postalCode: "", addressLine1: "", addressLine2: null,
  };
  const version = draft?.version ?? query.data?.version;
  const changedElsewhere = !mutation.isPending && draft !== null && query.data !== undefined && draft.version !== query.data.version;
  const needsReload = changedElsewhere || conflict;
  const writeDisabled = mutation.isPending || query.isError || needsReload;
  const deleting = mutation.isPending && mutation.variables?.shippingAddress === null;
  const loadLatest = () => {
    void runForCurrentCustomer(
      () => query.refetch({ throwOnError: true }),
      () => { setDraft(null); mutation.reset(); },
    ).catch(() => { /* 조회 실패 시 입력을 유지한다. */ });
  };
  return <Card id="my-default-shipping-address" className="mb-4"><Card.Body>
    <h6>기본 배송지</h6>
    <p className="small text-muted">다음 주문에서 불러올 주소입니다. 이미 접수한 주문의 배송지는 변경되지 않습니다.</p>
    {query.isLoading && <LoadingSpinner />}
    <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }} retrying={query.isFetching} />
    {needsReload && <Alert variant="warning">
      기본 배송지가 다른 화면에서 변경되었습니다. 입력 내용은 유지했습니다.
      <div className="mt-2"><Button size="sm" variant="outline-secondary" disabled={query.isFetching || mutation.isPending} onClick={loadLatest}>최신 주소 불러오기</Button></div>
    </Alert>}
    {!conflict && <ErrorAlert error={mutation.error} onRetry={needsReload ? undefined : retry} retrying={mutation.isPending} />}
    {query.data && <Form onSubmit={(event) => {
      event.preventDefault();
      if (version !== undefined && !writeDisabled && isFulfillmentComplete({ fulfillmentType: "SHIPPING", shippingAddress: address })) change(address, version);
    }}>
      <fieldset disabled={mutation.isPending}>
        <ShippingAddressFields value={address} onChange={(shippingAddress) => {
          if (version === undefined) return;
          setDraft({ version, shippingAddress });
          if (!conflict) mutation.reset();
        }} />
        <div className="d-flex flex-wrap gap-2 mt-3">
          <Button type="submit" disabled={writeDisabled || !isFulfillmentComplete({ fulfillmentType: "SHIPPING", shippingAddress: address })}>
            {mutation.isPending && !deleting ? "저장 중..." : "기본 배송지 저장"}
          </Button>
          {query.data.shippingAddress && <Button variant="outline-secondary" disabled={writeDisabled} onClick={() => {
            if (version !== undefined) change(null, version);
          }}>{deleting ? "삭제 중..." : "기본 배송지 삭제"}</Button>}
        </div>
      </fieldset>
    </Form>}
  </Card.Body></Card>;
}
