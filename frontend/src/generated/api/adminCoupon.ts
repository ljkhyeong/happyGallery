import { generatedApiClient } from '../../shared/api/generatedClient';
export type AdminCouponResponseDiscountType = typeof AdminCouponResponseDiscountType[keyof typeof AdminCouponResponseDiscountType];


export const AdminCouponResponseDiscountType = {
  FIXED: 'FIXED',
  PERCENT: 'PERCENT',
} as const;

export interface AdminCouponResponse {
  active: boolean;
  discountType: AdminCouponResponseDiscountType;
  discountValue: number;
  id: number;
  /** @nullable */
  maxDiscountAmount: number | null;
  minOrderAmount: number;
  name: string;
  publiclyClaimable: boolean;
  validFrom: string;
  validUntil: string;
  version: number;
}

export type CreateCouponRequestDiscountType = typeof CreateCouponRequestDiscountType[keyof typeof CreateCouponRequestDiscountType];


export const CreateCouponRequestDiscountType = {
  FIXED: 'FIXED',
  PERCENT: 'PERCENT',
} as const;

export interface CreateCouponRequest {
  active: boolean;
  discountType: CreateCouponRequestDiscountType;
  /** @maximum 9007199254740991 */
  discountValue: number;
  /**
     * @maximum 9007199254740991
     * @nullable
     */
  maxDiscountAmount?: number | null;
  /** @maximum 9007199254740991 */
  minOrderAmount: number;
  /**
     * @minLength 0
     * @maxLength 100
     */
  name: string;
  publiclyClaimable: boolean;
  validFrom: string;
  validUntil: string;
}

export type UpdateCouponRequestDiscountType = typeof UpdateCouponRequestDiscountType[keyof typeof UpdateCouponRequestDiscountType];


export const UpdateCouponRequestDiscountType = {
  FIXED: 'FIXED',
  PERCENT: 'PERCENT',
} as const;

export interface UpdateCouponRequest {
  active: boolean;
  discountType: UpdateCouponRequestDiscountType;
  /** @maximum 9007199254740991 */
  discountValue: number;
  expectedVersion: number;
  /**
     * @maximum 9007199254740991
     * @nullable
     */
  maxDiscountAmount?: number | null;
  /** @maximum 9007199254740991 */
  minOrderAmount: number;
  /**
     * @minLength 0
     * @maxLength 100
     */
  name: string;
  publiclyClaimable: boolean;
  validFrom: string;
  validUntil: string;
}

export type DeleteAdminCouponParams = {
/**
 * @minimum 0
 */
expectedVersion: number;
};

export const getListAdminCouponsUrl = () => {




  return `/api/v1/admin/coupons`
}

export const listAdminCoupons = async ( options?: RequestInit): Promise<AdminCouponResponse[]> => {

  return generatedApiClient<AdminCouponResponse[]>(getListAdminCouponsUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCreateAdminCouponUrl = () => {




  return `/api/v1/admin/coupons`
}

export const createAdminCoupon = async (createCouponRequest: CreateCouponRequest, options?: RequestInit): Promise<AdminCouponResponse> => {

  return generatedApiClient<AdminCouponResponse>(getCreateAdminCouponUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(createCouponRequest)
  }
);}



export const getDeleteAdminCouponUrl = (id: number,
    params: DeleteAdminCouponParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/coupons/${id}?${stringifiedParams}` : `/api/v1/admin/coupons/${id}`
}

export const deleteAdminCoupon = async (id: number,
    params: DeleteAdminCouponParams, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getDeleteAdminCouponUrl(id,params),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getGetAdminCouponUrl = (id: number,) => {




  return `/api/v1/admin/coupons/${id}`
}

export const getAdminCoupon = async (id: number, options?: RequestInit): Promise<AdminCouponResponse> => {

  return generatedApiClient<AdminCouponResponse>(getGetAdminCouponUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getUpdateAdminCouponUrl = (id: number,) => {




  return `/api/v1/admin/coupons/${id}`
}

export const updateAdminCoupon = async (id: number,
    updateCouponRequest: UpdateCouponRequest, options?: RequestInit): Promise<AdminCouponResponse> => {

  return generatedApiClient<AdminCouponResponse>(getUpdateAdminCouponUrl(id),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateCouponRequest)
  }
);}
