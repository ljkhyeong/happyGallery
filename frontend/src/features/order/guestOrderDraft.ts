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
interface GuestOrderDraft { productId: number | null; items: OrderItemInput[] }
type GuestOrderDraftHandle = CustomerSessionStorageHandle<GuestOrderDraft>;

export function saveGuestOrderDraft(productId: number | null, items: OrderItemInput[]): string | null {
  const owner = currentCustomerSessionStorageOwner();
  if (!owner) return null;
  const id = crypto.randomUUID();
  return writeGuestOrderDraft(id, { owner, value: { productId, items } })
    ? id : null;
}

export function writeGuestOrderDraft(id: string, draft: GuestOrderDraftHandle): boolean {
  return isCurrentCustomerSessionStorageOwner(draft.owner)
    && writeSessionValue(`${DRAFT_KEY_PREFIX}${id}`, JSON.stringify(draft));
}

export function readGuestOrderDraft(id: string | null, productId: number | null): GuestOrderDraftHandle | null {
  if (!id) return null;
  try {
    const raw = readSessionValue(`${DRAFT_KEY_PREFIX}${id}`);
    if (!raw) return null;
    const draft = JSON.parse(raw) as GuestOrderDraftHandle;
    if (!isCustomerSessionStorageOwner(draft.owner) || !isCurrentCustomerSessionStorageOwner(draft.owner)
      || draft.value?.productId !== productId || !Array.isArray(draft.value.items)) return null;
    return draft.value.items.every((item) => Number.isSafeInteger(item?.productId) && item.productId > 0
      && (item.productVariantId === null
        || (Number.isSafeInteger(item.productVariantId) && Number(item.productVariantId) > 0))
      && Number.isInteger(item.qty) && item.qty >= 1 && item.qty <= MAX_PRODUCT_QUANTITY
      && Array.isArray(item.textInputs) && item.textInputs.every((input) => (
        typeof input?.groupKey === "string" && typeof input.value === "string"
      ))) ? draft : null;
  } catch {
    return null;
  }
}
