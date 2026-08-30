import type { OrderItemInput } from "@/shared/types";
import { readSessionValue, writeSessionValue } from "@/shared/storage/browserSessionStorage";
import {
  currentCustomerSessionStorageOwner,
  isCurrentCustomerSessionStorageOwner,
  isCustomerSessionStorageOwner,
  type CustomerSessionStorageHandle,
} from "@/shared/storage/customerSessionOwner";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";

const DRAFT_KEY_PREFIX = "hg_guest_order_draft:";
interface GuestOrderDraft { productId: number; items: OrderItemInput[] }

export function saveGuestOrderDraft(productId: number, items: OrderItemInput[]): string | null {
  const owner = currentCustomerSessionStorageOwner();
  if (!owner) return null;
  const id = crypto.randomUUID();
  return writeSessionValue(`${DRAFT_KEY_PREFIX}${id}`, JSON.stringify({ owner, value: { productId, items } }))
    ? id : null;
}

export function readGuestOrderDraft(id: string | null, productId: number): OrderItemInput[] | null {
  if (!id) return null;
  try {
    const raw = readSessionValue(`${DRAFT_KEY_PREFIX}${id}`);
    if (!raw) return null;
    const draft = JSON.parse(raw) as CustomerSessionStorageHandle<GuestOrderDraft>;
    if (!isCustomerSessionStorageOwner(draft.owner) || !isCurrentCustomerSessionStorageOwner(draft.owner)
      || draft.value?.productId !== productId || !Array.isArray(draft.value.items)
      || draft.value.items.length === 0) return null;
    return draft.value.items.every((item) => item?.productId === productId
      && Number.isSafeInteger(item.productVariantId) && Number(item.productVariantId) > 0
      && Number.isInteger(item.qty) && item.qty >= 1 && item.qty <= MAX_PRODUCT_QUANTITY
      && Array.isArray(item.textInputs) && item.textInputs.every((input) => (
        typeof input?.groupKey === "string" && typeof input.value === "string"
      ))) ? draft.value.items : null;
  } catch {
    return null;
  }
}
