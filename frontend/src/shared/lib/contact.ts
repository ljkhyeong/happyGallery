// SEO JSON-LD(node 단위 테스트에서 직접 import)도 쓰므로 @/ 별칭 import를 두지 않는다.

interface AddressLines {
  addressLine1?: string | null;
  addressLine2?: string | null;
}

/** 공방 주소 한 줄: 기본 주소와 상세 주소를 공백으로 잇고 빈 값은 뺀다. */
export function workshopAddress({ addressLine1, addressLine2 }: AddressLines): string {
  return [addressLine1, addressLine2].filter(Boolean).join(" ");
}

/** 숫자만 남긴 전화 링크 주소. */
export function telHref(phone: string): string {
  return `tel:${phone.replace(/\D/g, "")}`;
}
