import { MySectionPage } from "@/features/my/MySectionPage";
import { MyDefaultShippingAddressSection } from "@/features/my/DefaultShippingAddress";

export function MyShippingAddressPage() {
  return <MySectionPage title="기본 배송지"><MyDefaultShippingAddressSection /></MySectionPage>;
}
