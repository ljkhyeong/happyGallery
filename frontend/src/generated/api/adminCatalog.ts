import { generatedApiClient } from '../../shared/api/generatedClient';
export type AdminClassResponseStatus = typeof AdminClassResponseStatus[keyof typeof AdminClassResponseStatus];


export const AdminClassResponseStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
} as const;

export interface AdminClassResponse {
  bufferMin: number;
  category: string;
  /** @nullable */
  description: string | null;
  durationMin: number;
  id: number;
  /** @nullable */
  imageUrl: string | null;
  name: string;
  passEligible: boolean;
  /** @nullable */
  preparationInfo: string | null;
  price: number;
  status: AdminClassResponseStatus;
  /** @nullable */
  targetAudience: string | null;
}

export interface CreateClassRequest {
  bufferMin: number;
  /**
     * @minLength 0
     * @maxLength 30
     */
  category: string;
  /**
     * @minLength 0
     * @maxLength 5000
     */
  description?: string;
  durationMin: number;
  /**
     * @minLength 0
     * @maxLength 500
     */
  imageUrl?: string;
  /**
     * @minLength 0
     * @maxLength 100
     */
  name: string;
  passEligible: boolean;
  /**
     * @minLength 0
     * @maxLength 2000
     */
  preparationInfo?: string;
  /**
     * @minimum 10
     * @maximum 9007199254740991
     */
  price: number;
  /**
     * @minLength 0
     * @maxLength 1000
     */
  targetAudience?: string;
}

export interface UpdateClassRequest {
  /**
     * @minLength 0
     * @maxLength 30
     */
  category: string;
  /**
     * @minLength 0
     * @maxLength 5000
     */
  description?: string;
  /**
     * @minLength 0
     * @maxLength 500
     */
  imageUrl?: string;
  /**
     * @minLength 0
     * @maxLength 100
     */
  name: string;
  passEligible: boolean;
  /**
     * @minLength 0
     * @maxLength 2000
     */
  preparationInfo?: string;
  /**
     * @minimum 10
     * @maximum 9007199254740991
     */
  price: number;
  /**
     * @minLength 0
     * @maxLength 1000
     */
  targetAudience?: string;
}

export type UpdateClassStatusRequestStatus = typeof UpdateClassStatusRequestStatus[keyof typeof UpdateClassStatusRequestStatus];


export const UpdateClassStatusRequestStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
} as const;

export interface UpdateClassStatusRequest {
  status: UpdateClassStatusRequestStatus;
}

export interface ImageUploadResponse {
  url: string;
}

export type ProductResponseStatus = typeof ProductResponseStatus[keyof typeof ProductResponseStatus];


export const ProductResponseStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
} as const;

export type ProductResponseType = typeof ProductResponseType[keyof typeof ProductResponseType];


export const ProductResponseType = {
  READY_STOCK: 'READY_STOCK',
  MADE_TO_ORDER: 'MADE_TO_ORDER',
} as const;

export interface ProductResponse {
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
  price: number;
  /** @nullable */
  productionLeadDays: number | null;
  quantity: number;
  /** @nullable */
  specification: string | null;
  status: ProductResponseStatus;
  type: ProductResponseType;
}

export type CreateProductRequestType = typeof CreateProductRequestType[keyof typeof CreateProductRequestType];


export const CreateProductRequestType = {
  READY_STOCK: 'READY_STOCK',
  MADE_TO_ORDER: 'MADE_TO_ORDER',
} as const;

export interface CreateProductRequest {
  /**
     * @minLength 0
     * @maxLength 2000
     */
  careInstructions?: string;
  /**
     * @minLength 0
     * @maxLength 50
     */
  category?: string;
  /**
     * @minLength 0
     * @maxLength 5000
     */
  description?: string;
  /**
     * @minLength 0
     * @maxLength 500
     */
  imageUrl?: string;
  /**
     * @minLength 0
     * @maxLength 100
     */
  name: string;
  /** @maximum 9007199254740991 */
  price: number;
  /**
     * @minimum 1
     * @maximum 180
     */
  productionLeadDays?: number;
  /** @minimum 1 */
  quantity: number;
  /**
     * @minLength 0
     * @maxLength 2000
     */
  specification?: string;
  type: CreateProductRequestType;
}

export interface UpdateProductRequest {
  /**
     * @minLength 0
     * @maxLength 2000
     */
  careInstructions?: string;
  /**
     * @minLength 0
     * @maxLength 50
     */
  category?: string;
  /**
     * @minLength 0
     * @maxLength 5000
     */
  description?: string;
  /**
     * @minLength 0
     * @maxLength 500
     */
  imageUrl?: string;
  /**
     * @minLength 0
     * @maxLength 100
     */
  name: string;
  /** @maximum 9007199254740991 */
  price: number;
  /**
     * @minimum 1
     * @maximum 180
     */
  productionLeadDays?: number;
  /**
     * @minLength 0
     * @maxLength 2000
     */
  specification?: string;
}

export type InventoryAdjustmentResponseType = typeof InventoryAdjustmentResponseType[keyof typeof InventoryAdjustmentResponseType];


export const InventoryAdjustmentResponseType = {
  INCREASE: 'INCREASE',
  DECREASE: 'DECREASE',
} as const;

export interface InventoryAdjustmentResponse {
  adjustedAt: string;
  adjustedBy: string;
  /** @nullable */
  adjustedByAdminId: number | null;
  id: number;
  productId: number;
  quantity: number;
  quantityAfter: number;
  quantityBefore: number;
  reason: string;
  type: InventoryAdjustmentResponseType;
}

export type AdjustInventoryRequestType = typeof AdjustInventoryRequestType[keyof typeof AdjustInventoryRequestType];


export const AdjustInventoryRequestType = {
  INCREASE: 'INCREASE',
  DECREASE: 'DECREASE',
} as const;

export interface AdjustInventoryRequest {
  quantity: number;
  /**
     * @minLength 0
     * @maxLength 500
     */
  reason: string;
  type: AdjustInventoryRequestType;
}

export type UpdateProductStatusRequestStatus = typeof UpdateProductStatusRequestStatus[keyof typeof UpdateProductStatusRequestStatus];


export const UpdateProductStatusRequestStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
} as const;

export interface UpdateProductStatusRequest {
  status: UpdateProductStatusRequestStatus;
}

export interface SlotResponse {
  adminActive: boolean;
  bookedCount: number;
  bufferBlocked: boolean;
  capacity: number;
  classId: number;
  endAt: string;
  id: number;
  isActive: boolean;
  startAt: string;
}

export interface CreateSlotRequest {
  classId: number;
  startAt: string;
}

export type BulkSlotRequestWeekdaysItem = typeof BulkSlotRequestWeekdaysItem[keyof typeof BulkSlotRequestWeekdaysItem];


export const BulkSlotRequestWeekdaysItem = {
  MONDAY: 'MONDAY',
  TUESDAY: 'TUESDAY',
  WEDNESDAY: 'WEDNESDAY',
  THURSDAY: 'THURSDAY',
  FRIDAY: 'FRIDAY',
  SATURDAY: 'SATURDAY',
  SUNDAY: 'SUNDAY',
} as const;

export interface BulkSlotRequest {
  classId: number;
  dateFrom: string;
  dateTo: string;
  /**
     * @minItems 0
     * @maxItems 24
     */
  startTimes: string[];
  /**
     * @minItems 0
     * @maxItems 7
     */
  weekdays: BulkSlotRequestWeekdaysItem[];
}

export type BulkSlotItemResponseStatus = typeof BulkSlotItemResponseStatus[keyof typeof BulkSlotItemResponseStatus];


export const BulkSlotItemResponseStatus = {
  CREATABLE: 'CREATABLE',
  CREATED: 'CREATED',
  SKIPPED_DUPLICATE: 'SKIPPED_DUPLICATE',
  SKIPPED_PAST: 'SKIPPED_PAST',
} as const;

export interface BulkSlotItemResponse {
  bufferBlocked: boolean;
  endAt: string;
  /** @nullable */
  slotId: number | null;
  startAt: string;
  status: BulkSlotItemResponseStatus;
}

export interface BulkSlotResponse {
  creatableCount: number;
  createdCount: number;
  items: BulkSlotItemResponse[];
  skippedCount: number;
  totalCount: number;
}

export interface AdminSlotSessionCancelRequest {
  /**
     * @minLength 0
     * @maxLength 200
     */
  reason: string;
}

export interface AdminSlotSessionCancelResponse {
  balanceSettlementsRequired: number;
  canceledBookings: number;
  depositRefundsRequested: number;
  manualCompensationsRequired: number;
  passCreditsRestored: number;
}

export type UploadImageBody = {
  file: Blob;
};

export type ListSlotsParams = {
classId: number;
};

export const getListClassesUrl = () => {




  return `/api/v1/admin/classes`
}

export const listClasses = async ( options?: RequestInit): Promise<AdminClassResponse[]> => {

  return generatedApiClient<AdminClassResponse[]>(getListClassesUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCreateClassUrl = () => {




  return `/api/v1/admin/classes`
}

export const createClass = async (createClassRequest: CreateClassRequest, options?: RequestInit): Promise<AdminClassResponse> => {

  return generatedApiClient<AdminClassResponse>(getCreateClassUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(createClassRequest)
  }
);}



export const getUpdateClassUrl = (id: number,) => {




  return `/api/v1/admin/classes/${id}`
}

export const updateClass = async (id: number,
    updateClassRequest: UpdateClassRequest, options?: RequestInit): Promise<AdminClassResponse> => {

  return generatedApiClient<AdminClassResponse>(getUpdateClassUrl(id),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateClassRequest)
  }
);}



export const getChangeAdminClassStatusUrl = (id: number,) => {




  return `/api/v1/admin/classes/${id}/status`
}

export const changeAdminClassStatus = async (id: number,
    updateClassStatusRequest: UpdateClassStatusRequest, options?: RequestInit): Promise<AdminClassResponse> => {

  return generatedApiClient<AdminClassResponse>(getChangeAdminClassStatusUrl(id),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateClassStatusRequest)
  }
);}



export const getUploadImageUrl = () => {




  return `/api/v1/admin/media/images`
}

export const uploadImage = async (uploadImageBody?: UploadImageBody, options?: RequestInit): Promise<ImageUploadResponse> => {
    const formData = new FormData();
if(uploadImageBody?.file !== undefined) {
 formData.append(`file`, uploadImageBody.file);
 }

  return generatedApiClient<ImageUploadResponse>(getUploadImageUrl(),
  {
    ...options,
    method: 'POST'
    ,
    body: formData
  }
);}



export const getListAllUrl = () => {




  return `/api/v1/admin/products`
}

export const listAll = async ( options?: RequestInit): Promise<ProductResponse[]> => {

  return generatedApiClient<ProductResponse[]>(getListAllUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRegisterUrl = () => {




  return `/api/v1/admin/products`
}

export const register = async (createProductRequest: CreateProductRequest, options?: RequestInit): Promise<ProductResponse> => {

  return generatedApiClient<ProductResponse>(getRegisterUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(createProductRequest)
  }
);}



export const getUpdateAdminProductUrl = (id: number,) => {




  return `/api/v1/admin/products/${id}`
}

export const updateAdminProduct = async (id: number,
    updateProductRequest: UpdateProductRequest, options?: RequestInit): Promise<ProductResponse> => {

  return generatedApiClient<ProductResponse>(getUpdateAdminProductUrl(id),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateProductRequest)
  }
);}



export const getListInventoryAdjustmentsUrl = (id: number,) => {




  return `/api/v1/admin/products/${id}/inventory-adjustments`
}

export const listInventoryAdjustments = async (id: number, options?: RequestInit): Promise<InventoryAdjustmentResponse[]> => {

  return generatedApiClient<InventoryAdjustmentResponse[]>(getListInventoryAdjustmentsUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getAdjustInventoryUrl = (id: number,) => {




  return `/api/v1/admin/products/${id}/inventory-adjustments`
}

export const adjustInventory = async (id: number,
    adjustInventoryRequest: AdjustInventoryRequest, options?: RequestInit): Promise<InventoryAdjustmentResponse> => {

  return generatedApiClient<InventoryAdjustmentResponse>(getAdjustInventoryUrl(id),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(adjustInventoryRequest)
  }
);}



export const getChangeStatusUrl = (id: number,) => {




  return `/api/v1/admin/products/${id}/status`
}

export const changeStatus = async (id: number,
    updateProductStatusRequest: UpdateProductStatusRequest, options?: RequestInit): Promise<ProductResponse> => {

  return generatedApiClient<ProductResponse>(getChangeStatusUrl(id),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateProductStatusRequest)
  }
);}



export const getListSlotsUrl = (params: ListSlotsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/slots?${stringifiedParams}` : `/api/v1/admin/slots`
}

export const listSlots = async (params: ListSlotsParams, options?: RequestInit): Promise<SlotResponse[]> => {

  return generatedApiClient<SlotResponse[]>(getListSlotsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCreateSlotUrl = () => {




  return `/api/v1/admin/slots`
}

export const createSlot = async (createSlotRequest: CreateSlotRequest, options?: RequestInit): Promise<SlotResponse> => {

  return generatedApiClient<SlotResponse>(getCreateSlotUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(createSlotRequest)
  }
);}



export const getCreateBulkSlotsUrl = () => {




  return `/api/v1/admin/slots/bulk`
}

export const createBulkSlots = async (bulkSlotRequest: BulkSlotRequest, options?: RequestInit): Promise<BulkSlotResponse> => {

  return generatedApiClient<BulkSlotResponse>(getCreateBulkSlotsUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(bulkSlotRequest)
  }
);}



export const getPreviewBulkSlotsUrl = () => {




  return `/api/v1/admin/slots/bulk/preview`
}

export const previewBulkSlots = async (bulkSlotRequest: BulkSlotRequest, options?: RequestInit): Promise<BulkSlotResponse> => {

  return generatedApiClient<BulkSlotResponse>(getPreviewBulkSlotsUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(bulkSlotRequest)
  }
);}



export const getActivateSlotUrl = (id: number,) => {




  return `/api/v1/admin/slots/${id}/activate`
}

export const activateSlot = async (id: number, options?: RequestInit): Promise<SlotResponse> => {

  return generatedApiClient<SlotResponse>(getActivateSlotUrl(id),
  {
    ...options,
    method: 'PATCH'


  }
);}



export const getDeactivateSlotUrl = (id: number,) => {




  return `/api/v1/admin/slots/${id}/deactivate`
}

export const deactivateSlot = async (id: number, options?: RequestInit): Promise<SlotResponse> => {

  return generatedApiClient<SlotResponse>(getDeactivateSlotUrl(id),
  {
    ...options,
    method: 'PATCH'


  }
);}



export const getCancelAdminSlotSessionUrl = (slotId: number,) => {




  return `/api/v1/admin/slots/${slotId}/cancel-session`
}

export const cancelAdminSlotSession = async (slotId: number,
    adminSlotSessionCancelRequest: AdminSlotSessionCancelRequest, options?: RequestInit): Promise<AdminSlotSessionCancelResponse> => {

  return generatedApiClient<AdminSlotSessionCancelResponse>(getCancelAdminSlotSessionUrl(slotId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(adminSlotSessionCancelRequest)
  }
);}
