import { useEffect, useState } from "react";
import type { FulfillmentType, ShippingAddress } from "@/features/payment";
import { isValidPhone, normalizePhone } from "@/shared/validation/phone";

export interface FulfillmentSelection {
  fulfillmentType: FulfillmentType | null;
  shippingAddress: ShippingAddress;
}

const emptyAddress: ShippingAddress = {
  recipientName: "",
  phone: "",
  postalCode: "",
  addressLine1: "",
  addressLine2: null,
};

function initialFulfillmentSelection(
  defaultName?: string,
  defaultPhone?: string,
): FulfillmentSelection {
  return {
    fulfillmentType: null,
    shippingAddress: {
      ...emptyAddress,
      recipientName: defaultName ?? "",
      phone: normalizePhone(defaultPhone),
    },
  };
}

export function useFulfillmentSelection(
  defaultName?: string,
  defaultPhone?: string,
) {
  const [selection, setSelection] = useState<FulfillmentSelection>(
    () => initialFulfillmentSelection(defaultName, defaultPhone),
  );

  useEffect(() => {
    setSelection((current) => ({
      ...current,
      shippingAddress: {
        ...current.shippingAddress,
        recipientName: current.shippingAddress.recipientName || defaultName || "",
        phone: current.shippingAddress.phone || normalizePhone(defaultPhone),
      },
    }));
  }, [defaultName, defaultPhone]);

  return [selection, setSelection] as const;
}

export function isFulfillmentComplete(selection: FulfillmentSelection) {
  if (selection.fulfillmentType === "PICKUP") return true;
  if (selection.fulfillmentType !== "SHIPPING") return false;
  const address = selection.shippingAddress;
  return Boolean(
    address.recipientName.trim()
      && isValidPhone(normalizePhone(address.phone))
      && /^\d{5}$/.test(address.postalCode.trim())
      && address.addressLine1.trim(),
  );
}

export function fulfillmentPayload(selection: FulfillmentSelection) {
  if (!selection.fulfillmentType) {
    throw new Error("수령 방법을 선택해 주세요.");
  }
  return {
    fulfillmentType: selection.fulfillmentType,
    shippingAddress: selection.fulfillmentType === "SHIPPING"
      ? {
          ...selection.shippingAddress,
          phone: normalizePhone(selection.shippingAddress.phone),
          addressLine2: selection.shippingAddress.addressLine2?.trim() || null,
        }
      : null,
  };
}

