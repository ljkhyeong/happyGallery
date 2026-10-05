import { MySectionPage } from "@/features/my/MySectionPage";
import { MyDefaultShippingAddressSection } from "@/features/my/DefaultShippingAddress";

export function MyShippingAddressPage() {
  return <MySectionPage path="/my/shipping-address"><MyDefaultShippingAddressSection /></MySectionPage>;
}
