import type { OrderItemInput } from "@/shared/types";
import { writeSessionValue } from "@/shared/storage/browserSessionStorage";
import {
  isCurrentCustomerSessionStorageOwner,
  ownedSessionValue,
  readOwnedSessionValue,
  type CustomerSessionStorageHandle,
} from "@/shared/storage/customerSessionOwner";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";

const DRAFT_KEY_PREFIX = "hg_guest_order_draft:";
interface GuestOrderDraft { productId: number | null; items: OrderItemInput[] }
type GuestOrderDraftHandle = CustomerSessionStorageHandle<GuestOrderDraft>;

export function saveGuestOrderDraft(productId: number | null, items: OrderItemInput[]): string | null {
  const draft = ownedSessionValue({ productId, items });
  if (!draft) return null;
  const id = crypto.randomUUID();
  return writeGuestOrderDraft(id, draft) ? id : null;
}

export function writeGuestOrderDraft(id: string, draft: GuestOrderDraftHandle): boolean {
  return isCurrentCustomerSessionStorageOwner(draft.owner)
    && writeSessionValue(`${DRAFT_KEY_PREFIX}${id}`, JSON.stringify(draft));
}

function isGuestOrderDraftItem(value: unknown): value is OrderItemInput {
  if (typeof value !== "object" || value === null) return false;
  const item = value as OrderItemInput;
  return Number.isSafeInteger(item.productId) && item.productId > 0
    && (item.productVariantId === null
      || (Number.isSafeInteger(item.productVariantId) && Number(item.productVariantId) > 0))
    && Number.isInteger(item.qty) && item.qty >= 1 && item.qty <= MAX_PRODUCT_QUANTITY
    && Array.isArray(item.textInputs) && item.textInputs.every((input) => (
      typeof input?.groupKey === "string" && typeof input.value === "string"
    ));
}

export function readGuestOrderDraft(id: string | null, productId: number | null): GuestOrderDraftHandle | null {
  if (!id) return null;
  const isDraftForProduct = (value: unknown): value is GuestOrderDraft => {
    const draft = value as Partial<GuestOrderDraft> | undefined;
    return draft?.productId === productId
      && Array.isArray(draft.items)
      && draft.items.every(isGuestOrderDraftItem);
  };
  return readOwnedSessionValue(`${DRAFT_KEY_PREFIX}${id}`, isDraftForProduct);
}
