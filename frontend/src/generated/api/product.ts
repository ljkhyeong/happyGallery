import { generatedApiClient } from '../../shared/api/generatedClient';
export type ProductDetailResponseType = typeof ProductDetailResponseType[keyof typeof ProductDetailResponseType];


export const ProductDetailResponseType = {
  READY_STOCK: 'READY_STOCK',
  MADE_TO_ORDER: 'MADE_TO_ORDER',
} as const;

export type ProductOptionGroupResponseType = typeof ProductOptionGroupResponseType[keyof typeof ProductOptionGroupResponseType];


export const ProductOptionGroupResponseType = {
  SELECT: 'SELECT',
  TEXT: 'TEXT',
} as const;

export interface ProductOptionValueResponse {
  key: string;
  name: string;
  sortOrder: number;
}

export interface ProductOptionGroupResponse {
  /** @nullable */
  inputMaxLength: number | null;
  /** @nullable */
  inputPlaceholder: string | null;
  /** @nullable */
  inputPriceAdjustment: number | null;
  key: string;
  name: string;
  required: boolean;
  sortOrder: number;
  type: ProductOptionGroupResponseType;
  values: ProductOptionValueResponse[];
}

export interface ProductVariantSelectionResponse {
  groupKey: string;
  valueKey: string;
}

export interface ProductVariantResponse {
  active: boolean;
  id: number;
  priceAdjustment: number;
  quantity: number;
  selections: ProductVariantSelectionResponse[];
}

export interface ProductDetailResponse {
  available: boolean;
  /** @nullable */
  careInstructions: string | null;
  /** @nullable */
  category: string | null;
  /** @nullable */
  description: string | null;
  id: number;
  /** @nullable */
  imageUrl: string | null;
  name: string;
  optionGroups: ProductOptionGroupResponse[];
  price: number;
  /** @nullable */
  productionLeadDays: number | null;
  /** @nullable */
  specification: string | null;
  /**
     * 현재 재고 수량. 주문제작은 활성 옵션 조합의 합계
     * @minimum 0
     */
  stockQuantity: number;
  type: ProductDetailResponseType;
  variants: ProductVariantResponse[];
}

export type ListProductsParams = {
type?: ListProductsType;
category?: string;
keyword?: string;
sort?: ListProductsSort;
};

export type ListProductsType = typeof ListProductsType[keyof typeof ListProductsType];


export const ListProductsType = {
  READY_STOCK: 'READY_STOCK',
  MADE_TO_ORDER: 'MADE_TO_ORDER',
} as const;

export type ListProductsSort = typeof ListProductsSort[keyof typeof ListProductsSort];


export const ListProductsSort = {
  newest: 'newest',
  price_asc: 'price_asc',
  price_desc: 'price_desc',
} as const;

export const getListProductsUrl = (params?: ListProductsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/products?${stringifiedParams}` : `/api/v1/products`
}

export const listProducts = async (params?: ListProductsParams, options?: RequestInit): Promise<ProductDetailResponse[]> => {

  return generatedApiClient<ProductDetailResponse[]>(getListProductsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListProductCategoriesUrl = () => {




  return `/api/v1/products/categories`
}

export const listProductCategories = async ( options?: RequestInit): Promise<string[]> => {

  return generatedApiClient<string[]>(getListProductCategoriesUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetProductUrl = (id: number,) => {




  return `/api/v1/products/${id}`
}

export const getProduct = async (id: number, options?: RequestInit): Promise<ProductDetailResponse> => {

  return generatedApiClient<ProductDetailResponse>(getGetProductUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}
