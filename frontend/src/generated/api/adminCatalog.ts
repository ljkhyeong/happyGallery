import { generatedApiClient } from '../../shared/api/generatedClient';
export type AdminClassResponseStatus = typeof AdminClassResponseStatus[keyof typeof AdminClassResponseStatus];


export const AdminClassResponseStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
} as const;

export interface AdminClassResponse {
  bufferMin: number;
  /** @minimum 1 */
  capacity: number;
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
  /** @minimum 1 */
  capacity: number;
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
  optionGroups: ProductOptionGroupResponse[];
  price: number;
  /** @nullable */
  productionLeadDays: number | null;
  quantity: number;
  /** @nullable */
  specification: string | null;
  status: ProductResponseStatus;
  type: ProductResponseType;
  variants: ProductVariantResponse[];
}

export type CreateProductRequestType = typeof CreateProductRequestType[keyof typeof CreateProductRequestType];


export const CreateProductRequestType = {
  READY_STOCK: 'READY_STOCK',
  MADE_TO_ORDER: 'MADE_TO_ORDER',
} as const;

export type ProductOptionGroupRequestType = typeof ProductOptionGroupRequestType[keyof typeof ProductOptionGroupRequestType];


export const ProductOptionGroupRequestType = {
  SELECT: 'SELECT',
  TEXT: 'TEXT',
} as const;

export interface ProductOptionValueRequest {
  /**
     * @minLength 1
     * @pattern ^[A-Za-z0-9_-]{1,64}$
     */
  key: string;
  /**
     * @minLength 0
     * @maxLength 25
     */
  name: string;
  sortOrder?: number;
}

export interface ProductOptionGroupRequest {
  /**
     * @minimum 1
     * @maximum 200
     */
  inputMaxLength?: number;
  /**
     * @minLength 0
     * @maxLength 100
     */
  inputPlaceholder?: string;
  /** @maximum 9007199254740991 */
  inputPriceAdjustment?: number;
  /**
     * @minLength 1
     * @pattern ^[A-Za-z0-9_-]{1,64}$
     */
  key: string;
  /**
     * @minLength 0
     * @maxLength 25
     */
  name: string;
  required?: boolean;
  sortOrder?: number;
  type: ProductOptionGroupRequestType;
  /**
     * @minItems 0
     * @maxItems 500
     */
  values: ProductOptionValueRequest[];
}

export interface ProductVariantSelectionRequest {
  /**
     * @minLength 1
     * @pattern ^[A-Za-z0-9_-]{1,64}$
     */
  groupKey: string;
  /**
     * @minLength 1
     * @pattern ^[A-Za-z0-9_-]{1,64}$
     */
  valueKey: string;
}

export interface ProductVariantRequest {
  active: boolean;
  priceAdjustment: number;
  /** 신규 조합의 최초 재고. 이미 등록된 조합은 요청값과 관계없이 현재 재고를 유지한다. */
  quantity: number;
  /**
     * @minItems 0
     * @maxItems 3
     */
  selections: ProductVariantSelectionRequest[];
}

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
  /**
     * @minItems 0
     * @maxItems 8
     */
  optionGroups?: ProductOptionGroupRequest[];
  /** @maximum 9007199254740991 */
  price: number;
  /**
     * @minimum 1
     * @maximum 180
     */
  productionLeadDays?: number;
  /** @nullable */
  quantity?: number | null;
  /**
     * @minLength 0
     * @maxLength 2000
     */
  specification?: string;
  type: CreateProductRequestType;
  /**
     * @minItems 0
     * @maxItems 500
     */
  variants?: ProductVariantRequest[];
}

export interface Product {
  channelProductNo: number;
  /** @nullable */
  imageUrl: string | null;
  name: string;
  originProductNo: number;
  salePrice: number;
  status: string;
  /** @nullable */
  stockQuantity: number | null;
}

export interface SmartStoreProductCatalogPageResponse {
  page: number;
  products: Product[];
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Option {
  name: string;
  optionId: number;
  price: number;
  stockQuantity: number;
  usable: boolean;
}

export interface SmartStoreChannelProductResponse {
  options: Option[];
  originProductNo: number;
  salePrice: number;
  status: string;
}

export interface InspectionProduct {
  action: string;
  channelProductNo: number;
  reason: string;
  restorationRequestAvailable: boolean;
}

export interface SmartStoreInspectionPageResponse {
  page: number;
  products: InspectionProduct[];
  size: number;
  totalElements: number;
  totalPages: number;
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
  /**
     * @minItems 0
     * @maxItems 8
     */
  optionGroups?: ProductOptionGroupRequest[];
  /** @maximum 9007199254740991 */
  price: number;
  /**
     * @minimum 1
     * @maximum 180
     */
  productionLeadDays?: number;
  /**
     * 선택형 옵션과 variants가 없을 때 신규 기본 조합의 최초 재고. 기존 조합 재고는 변경하지 않는다.
     * @nullable
     */
  quantity?: number | null;
  /**
     * @minLength 0
     * @maxLength 2000
     */
  specification?: string;
  /**
     * @minItems 0
     * @maxItems 500
     */
  variants?: ProductVariantRequest[];
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
  /** @nullable */
  productVariantId: number | null;
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
  /** @nullable */
  productVariantId?: number | null;
  quantity: number;
  /**
     * @minLength 0
     * @maxLength 500
     */
  reason: string;
  type: AdjustInventoryRequestType;
}

/**
 * @nullable
 */
export type SmartStoreInventoryMappingResponseSyncStatus = typeof SmartStoreInventoryMappingResponseSyncStatus[keyof typeof SmartStoreInventoryMappingResponseSyncStatus] | null;


export const SmartStoreInventoryMappingResponseSyncStatus = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  SYNCED: 'SYNCED',
  FAILED: 'FAILED',
} as const;

export interface SmartStoreVariantMappingResponse {
  optionId: number;
  productVariantId: number;
}

export interface SmartStoreInventoryMappingResponse {
  attemptCount: number;
  enabled: boolean;
  /** @nullable */
  lastError: string | null;
  mappingVersion: number;
  originProductNo: number;
  productId: number;
  /** @nullable */
  syncStatus: SmartStoreInventoryMappingResponseSyncStatus;
  /** @nullable */
  syncedAt: string | null;
  variants: SmartStoreVariantMappingResponse[];
}

export interface SmartStoreVariantMappingRequest {
  optionId: number;
  productVariantId: number;
}

export interface SaveSmartStoreInventoryMappingRequest {
  enabled?: boolean;
  /** @nullable */
  expectedMappingVersion: number | null;
  originProductNo: number;
  previousOriginConfirmed: boolean;
  /**
     * @minItems 0
     * @maxItems 500
     */
  variants?: SmartStoreVariantMappingRequest[];
}

export interface OptionPreview {
  /** @nullable */
  channelPrice: number | null;
  /** @nullable */
  channelUsable: boolean | null;
  different: boolean;
  localPrice: number;
  localUsable: boolean;
  optionId: number;
  productVariantId: number;
}

export interface SmartStoreProductPreviewResponse {
  channelSalePrice: number;
  channelStatus: string;
  different: boolean;
  localSalePrice: number;
  localStatus: string;
  options: OptionPreview[];
  originProductNo: number;
  previewVersion: string;
  productId: number;
}

export interface ApplySmartStoreProductRequest {
  /** @minLength 1 */
  previewVersion: string;
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
  calendarActive: boolean;
  capacity: number;
  classId: number;
  endAt: string;
  id: number;
  isActive: boolean;
  startAt: string;
}

export type BookingCalendarDayResponseEffectiveAvailability = typeof BookingCalendarDayResponseEffectiveAvailability[keyof typeof BookingCalendarDayResponseEffectiveAvailability];


export const BookingCalendarDayResponseEffectiveAvailability = {
  OPEN: 'OPEN',
  CLOSED: 'CLOSED',
} as const;

export type BookingCalendarDayResponseOverrideMode = typeof BookingCalendarDayResponseOverrideMode[keyof typeof BookingCalendarDayResponseOverrideMode];


export const BookingCalendarDayResponseOverrideMode = {
  DEFAULT: 'DEFAULT',
  OPEN: 'OPEN',
  CLOSED: 'CLOSED',
} as const;

export interface BookingTimeBlockResponse {
  date: string;
  endTime: string;
  id: number;
  /** @nullable */
  reason?: string | null;
  startTime: string;
}

export interface BookingCalendarDayResponse {
  date: string;
  effectiveAvailability: BookingCalendarDayResponseEffectiveAvailability;
  overrideMode: BookingCalendarDayResponseOverrideMode;
  publicHoliday: boolean;
  /** @nullable */
  reason?: string | null;
  timeBlocks: BookingTimeBlockResponse[];
}

export interface BookingCalendarSettingsResponse {
  blockPublicHolidays: boolean;
  closeTime: string;
  openTime: string;
  slotIntervalMin: number;
  version: number;
}

export interface BookingCalendarResponse {
  days: BookingCalendarDayResponse[];
  settings: BookingCalendarSettingsResponse;
}

export type UpdateBookingCalendarDayRequestMode = typeof UpdateBookingCalendarDayRequestMode[keyof typeof UpdateBookingCalendarDayRequestMode];


export const UpdateBookingCalendarDayRequestMode = {
  DEFAULT: 'DEFAULT',
  OPEN: 'OPEN',
  CLOSED: 'CLOSED',
} as const;

export interface UpdateBookingCalendarDayRequest {
  mode: UpdateBookingCalendarDayRequestMode;
  /**
     * @minLength 0
     * @maxLength 200
     * @nullable
     */
  reason?: string | null;
}

export interface UpdateBookingCalendarSettingsRequest {
  blockPublicHolidays: boolean;
  closeTime: string;
  expectedVersion: number;
  openTime: string;
  /**
     * @minimum 10
     * @maximum 120
     */
  slotIntervalMin: number;
}

export interface CreateBookingTimeBlockRequest {
  date: string;
  endTime: string;
  /**
     * @minLength 0
     * @maxLength 200
     * @nullable
     */
  reason?: string | null;
  startTime: string;
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

export interface NoticeSummaryResponse {
  /** @nullable */
  displayEndDate: string | null;
  /** @nullable */
  displayStartDate: string | null;
  importantNotice: boolean;
  /** @nullable */
  importantNoticeEndDate: string | null;
  /** @nullable */
  importantNoticeStartDate: string | null;
  postCategoryType: string;
  sellerNoticeId: number;
  title: string;
  wholeNotice: boolean;
}

export interface SmartStoreNoticePageResponse {
  notices: NoticeSummaryResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type SaveSmartStoreNoticeRequestPostCategoryType = typeof SaveSmartStoreNoticeRequestPostCategoryType[keyof typeof SaveSmartStoreNoticeRequestPostCategoryType];


export const SaveSmartStoreNoticeRequestPostCategoryType = {
  ORDINARY: 'ORDINARY',
  EVENT: 'EVENT',
  DELIVERY: 'DELIVERY',
  PRODUCT: 'PRODUCT',
} as const;

export interface SaveSmartStoreNoticeRequest {
  /** @minLength 1 */
  detailContents: string;
  displayEndDate?: string;
  displayStartDate?: string;
  importantNotice?: boolean;
  importantNoticeEndDate?: string;
  importantNoticeStartDate?: string;
  popup?: boolean;
  popupEndDate?: string;
  popupStartDate?: string;
  postCategoryType: SaveSmartStoreNoticeRequestPostCategoryType;
  /** @minLength 1 */
  title: string;
  wholeNotice?: boolean;
}

export interface SmartStoreNoticeIdResponse {
  sellerNoticeId: number;
}

export interface SmartStoreNoticeResponse {
  detailContents: string;
  /** @nullable */
  displayEndDate: string | null;
  /** @nullable */
  displayStartDate: string | null;
  importantNotice: boolean;
  /** @nullable */
  importantNoticeEndDate: string | null;
  /** @nullable */
  importantNoticeStartDate: string | null;
  popup: boolean;
  /** @nullable */
  popupEndDate: string | null;
  /** @nullable */
  popupStartDate: string | null;
  postCategoryType: string;
  sellerNoticeId: number;
  title: string;
  wholeNotice: boolean;
}

export interface ApplySmartStoreNoticeRequest {
  /** @minItems 1 */
  channelProductNos: number[];
}

export type UploadImageBody = {
  file: Blob;
};

export type ListSmartStoreProductsParams = {
/**
 * @minimum 1
 */
page?: number;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
};

export type ListSmartStoreInspectionProductsParams = {
/**
 * @minimum 1
 */
page?: number;
/**
 * @minimum 10
 * @maximum 100
 */
size?: number;
};

export type DeleteSmartStoreInventoryMappingParams = {
expectedMappingVersion: number;
previousOriginConfirmed: boolean;
};

export type ListSlotsParams = {
classId: number;
};

export type GetAdminBookingCalendarParams = {
dateFrom: string;
dateTo: string;
};

export type ListSmartStoreProductNoticesParams = {
/**
 * @minimum 1
 */
page?: number;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
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

export const uploadImage = async (uploadImageBody: UploadImageBody, options?: RequestInit): Promise<ImageUploadResponse> => {
    const formData = new FormData();
formData.append(`file`, uploadImageBody.file);

  return generatedApiClient<ImageUploadResponse>(getUploadImageUrl(),
  {
    ...options,
    method: 'POST'
    ,
    body: formData
  }
);}



export const getGetAdminImageUrl = (fileName: string,) => {




  return `/api/v1/admin/media/images/${fileName}`
}

export const getAdminImage = async (fileName: string, options?: RequestInit): Promise<Blob> => {

  return generatedApiClient<Blob>(getGetAdminImageUrl(fileName),
  {
    ...options,
    method: 'GET'


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



export const getListSmartStoreProductsUrl = (params?: ListSmartStoreProductsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/products/smartstore-catalog?${stringifiedParams}` : `/api/v1/admin/products/smartstore-catalog`
}

export const listSmartStoreProducts = async (params?: ListSmartStoreProductsParams, options?: RequestInit): Promise<SmartStoreProductCatalogPageResponse> => {

  return generatedApiClient<SmartStoreProductCatalogPageResponse>(getListSmartStoreProductsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetSmartStoreProductUrl = (originProductNo: number,) => {




  return `/api/v1/admin/products/smartstore-catalog/${originProductNo}`
}

export const getSmartStoreProduct = async (originProductNo: number, options?: RequestInit): Promise<SmartStoreChannelProductResponse> => {

  return generatedApiClient<SmartStoreChannelProductResponse>(getGetSmartStoreProductUrl(originProductNo),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListSmartStoreInspectionProductsUrl = (params?: ListSmartStoreInspectionProductsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/products/smartstore-inspections?${stringifiedParams}` : `/api/v1/admin/products/smartstore-inspections`
}

export const listSmartStoreInspectionProducts = async (params?: ListSmartStoreInspectionProductsParams, options?: RequestInit): Promise<SmartStoreInspectionPageResponse> => {

  return generatedApiClient<SmartStoreInspectionPageResponse>(getListSmartStoreInspectionProductsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRestoreSmartStoreInspectionProductUrl = (channelProductNo: number,) => {




  return `/api/v1/admin/products/smartstore-inspections/${channelProductNo}/restore`
}

export const restoreSmartStoreInspectionProduct = async (channelProductNo: number, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getRestoreSmartStoreInspectionProductUrl(channelProductNo),
  {
    ...options,
    method: 'PUT'


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



export const getDeleteSmartStoreInventoryMappingUrl = (id: number,
    params: DeleteSmartStoreInventoryMappingParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/products/${id}/smartstore-inventory?${stringifiedParams}` : `/api/v1/admin/products/${id}/smartstore-inventory`
}

export const deleteSmartStoreInventoryMapping = async (id: number,
    params: DeleteSmartStoreInventoryMappingParams, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getDeleteSmartStoreInventoryMappingUrl(id,params),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getGetSmartStoreInventoryMappingUrl = (id: number,) => {




  return `/api/v1/admin/products/${id}/smartstore-inventory`
}

export const getSmartStoreInventoryMapping = async (id: number, options?: RequestInit): Promise<SmartStoreInventoryMappingResponse> => {

  return generatedApiClient<SmartStoreInventoryMappingResponse>(getGetSmartStoreInventoryMappingUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getSaveSmartStoreInventoryMappingUrl = (id: number,) => {




  return `/api/v1/admin/products/${id}/smartstore-inventory`
}

export const saveSmartStoreInventoryMapping = async (id: number,
    saveSmartStoreInventoryMappingRequest: SaveSmartStoreInventoryMappingRequest, options?: RequestInit): Promise<SmartStoreInventoryMappingResponse> => {

  return generatedApiClient<SmartStoreInventoryMappingResponse>(getSaveSmartStoreInventoryMappingUrl(id),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(saveSmartStoreInventoryMappingRequest)
  }
);}



export const getRetrySmartStoreInventorySyncUrl = (id: number,) => {




  return `/api/v1/admin/products/${id}/smartstore-inventory/retry`
}

export const retrySmartStoreInventorySync = async (id: number, options?: RequestInit): Promise<SmartStoreInventoryMappingResponse> => {

  return generatedApiClient<SmartStoreInventoryMappingResponse>(getRetrySmartStoreInventorySyncUrl(id),
  {
    ...options,
    method: 'POST'


  }
);}



export const getPreviewSmartStoreProductSyncUrl = (id: number,) => {




  return `/api/v1/admin/products/${id}/smartstore-product-preview`
}

export const previewSmartStoreProductSync = async (id: number, options?: RequestInit): Promise<SmartStoreProductPreviewResponse> => {

  return generatedApiClient<SmartStoreProductPreviewResponse>(getPreviewSmartStoreProductSyncUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getApplySmartStoreProductSyncUrl = (id: number,) => {




  return `/api/v1/admin/products/${id}/smartstore-product-sync`
}

export const applySmartStoreProductSync = async (id: number,
    applySmartStoreProductRequest: ApplySmartStoreProductRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getApplySmartStoreProductSyncUrl(id),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(applySmartStoreProductRequest)
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



export const getGetAdminBookingCalendarUrl = (params: GetAdminBookingCalendarParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/slots/calendar?${stringifiedParams}` : `/api/v1/admin/slots/calendar`
}

export const getAdminBookingCalendar = async (params: GetAdminBookingCalendarParams, options?: RequestInit): Promise<BookingCalendarResponse> => {

  return generatedApiClient<BookingCalendarResponse>(getGetAdminBookingCalendarUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getUpdateAdminBookingCalendarDayUrl = (date: string,) => {




  return `/api/v1/admin/slots/calendar/days/${date}`
}

export const updateAdminBookingCalendarDay = async (date: string,
    updateBookingCalendarDayRequest: UpdateBookingCalendarDayRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getUpdateAdminBookingCalendarDayUrl(date),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateBookingCalendarDayRequest)
  }
);}



export const getUpdateAdminBookingCalendarSettingsUrl = () => {




  return `/api/v1/admin/slots/calendar/settings`
}

export const updateAdminBookingCalendarSettings = async (updateBookingCalendarSettingsRequest: UpdateBookingCalendarSettingsRequest, options?: RequestInit): Promise<BookingCalendarSettingsResponse> => {

  return generatedApiClient<BookingCalendarSettingsResponse>(getUpdateAdminBookingCalendarSettingsUrl(),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateBookingCalendarSettingsRequest)
  }
);}



export const getCreateAdminBookingTimeBlockUrl = () => {




  return `/api/v1/admin/slots/calendar/time-blocks`
}

export const createAdminBookingTimeBlock = async (createBookingTimeBlockRequest: CreateBookingTimeBlockRequest, options?: RequestInit): Promise<BookingTimeBlockResponse> => {

  return generatedApiClient<BookingTimeBlockResponse>(getCreateAdminBookingTimeBlockUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(createBookingTimeBlockRequest)
  }
);}



export const getDeleteAdminBookingTimeBlockUrl = (id: number,) => {




  return `/api/v1/admin/slots/calendar/time-blocks/${id}`
}

export const deleteAdminBookingTimeBlock = async (id: number, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getDeleteAdminBookingTimeBlockUrl(id),
  {
    ...options,
    method: 'DELETE'


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



export const getListSmartStoreProductNoticesUrl = (params?: ListSmartStoreProductNoticesParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/smartstore-notices?${stringifiedParams}` : `/api/v1/admin/smartstore-notices`
}

export const listSmartStoreProductNotices = async (params?: ListSmartStoreProductNoticesParams, options?: RequestInit): Promise<SmartStoreNoticePageResponse> => {

  return generatedApiClient<SmartStoreNoticePageResponse>(getListSmartStoreProductNoticesUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCreateSmartStoreProductNoticeUrl = () => {




  return `/api/v1/admin/smartstore-notices`
}

export const createSmartStoreProductNotice = async (saveSmartStoreNoticeRequest: SaveSmartStoreNoticeRequest, options?: RequestInit): Promise<SmartStoreNoticeIdResponse> => {

  return generatedApiClient<SmartStoreNoticeIdResponse>(getCreateSmartStoreProductNoticeUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(saveSmartStoreNoticeRequest)
  }
);}



export const getDeleteSmartStoreProductNoticeUrl = (sellerNoticeId: number,) => {




  return `/api/v1/admin/smartstore-notices/${sellerNoticeId}`
}

export const deleteSmartStoreProductNotice = async (sellerNoticeId: number, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getDeleteSmartStoreProductNoticeUrl(sellerNoticeId),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getGetSmartStoreProductNoticeUrl = (sellerNoticeId: number,) => {




  return `/api/v1/admin/smartstore-notices/${sellerNoticeId}`
}

export const getSmartStoreProductNotice = async (sellerNoticeId: number, options?: RequestInit): Promise<SmartStoreNoticeResponse> => {

  return generatedApiClient<SmartStoreNoticeResponse>(getGetSmartStoreProductNoticeUrl(sellerNoticeId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getUpdateSmartStoreProductNoticeUrl = (sellerNoticeId: number,) => {




  return `/api/v1/admin/smartstore-notices/${sellerNoticeId}`
}

export const updateSmartStoreProductNotice = async (sellerNoticeId: number,
    saveSmartStoreNoticeRequest: SaveSmartStoreNoticeRequest, options?: RequestInit): Promise<SmartStoreNoticeIdResponse> => {

  return generatedApiClient<SmartStoreNoticeIdResponse>(getUpdateSmartStoreProductNoticeUrl(sellerNoticeId),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(saveSmartStoreNoticeRequest)
  }
);}



export const getApplySmartStoreProductNoticeUrl = (sellerNoticeId: number,) => {




  return `/api/v1/admin/smartstore-notices/${sellerNoticeId}/products`
}

export const applySmartStoreProductNotice = async (sellerNoticeId: number,
    applySmartStoreNoticeRequest: ApplySmartStoreNoticeRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getApplySmartStoreProductNoticeUrl(sellerNoticeId),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(applySmartStoreNoticeRequest)
  }
);}
