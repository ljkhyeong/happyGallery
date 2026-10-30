import { myNavLabel } from "@/features/my/myNavigation";
import { clientOnlyMeta } from "@/shared/seo/metadata";

export { MyShippingAddressPage as default } from "@/pages/MyShippingAddressPage";

export function meta() {
  return clientOnlyMeta(myNavLabel("/my/shipping-address"));
}
