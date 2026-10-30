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

/** 화면에 보이는 휴대폰 번호(010-1234-5678·010-123-4567). 숫자만 있는 휴대폰 번호가 아니면 받은 그대로 둔다. */
export function formatPhone(phone: string): string {
  const parts = /^(01\d)(\d{3,4})(\d{4})$/.exec(phone);
  return parts ? `${parts[1]}-${parts[2]}-${parts[3]}` : phone;
}
