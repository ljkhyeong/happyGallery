import { generatedApiClient } from '../../shared/api/generatedClient';
export interface WorkshopProfileResponse {
  /** @nullable */
  addressLine1: string | null;
  /** @nullable */
  addressLine2: string | null;
  /** @nullable */
  businessHours: string | null;
  /** @nullable */
  businessRegistrationNumber: string | null;
  /** @nullable */
  email: string | null;
  /** @nullable */
  instagramUrl: string | null;
  /** @nullable */
  introduction: string | null;
  /** @nullable */
  kakaoTalkId: string | null;
  /** @nullable */
  mailOrderRegistrationNumber: string | null;
  /** @nullable */
  mapUrl: string | null;
  name: string;
  /** @nullable */
  naverBlogUrl: string | null;
  /** @nullable */
  naverTalkUrl: string | null;
  /** @nullable */
  parkingInfo: string | null;
  /** @nullable */
  phone: string | null;
  /** @nullable */
  postalCode: string | null;
  /** @nullable */
  representativeName: string | null;
  /** @nullable */
  smartStoreUrl: string | null;
  updatedAt: string;
  version: number;
}

export interface UpdateWorkshopProfileRequest {
  /**
     * @minLength 0
     * @maxLength 200
     * @nullable
     */
  addressLine1?: string | null;
  /**
     * @minLength 0
     * @maxLength 200
     * @nullable
     */
  addressLine2?: string | null;
  /**
     * @minLength 0
     * @maxLength 1000
     * @nullable
     */
  businessHours?: string | null;
  /**
     * @nullable
     * @pattern ^\d{3}-\d{2}-\d{5}$
     */
  businessRegistrationNumber?: string | null;
  /**
     * @minLength 0
     * @maxLength 254
     * @nullable
     */
  email?: string | null;
  expectedVersion: number;
  /**
     * @minLength 0
     * @maxLength 500
     * @nullable
     * @pattern ^[hH][tT][tT][pP][sS]?://.+
     */
  instagramUrl?: string | null;
  /**
     * @minLength 0
     * @maxLength 2000
     * @nullable
     */
  introduction?: string | null;
  /**
     * @minLength 0
     * @maxLength 100
     * @nullable
     */
  kakaoTalkId?: string | null;
  /**
     * @minLength 0
     * @maxLength 100
     * @nullable
     */
  mailOrderRegistrationNumber?: string | null;
  /**
     * @minLength 0
     * @maxLength 500
     * @nullable
     * @pattern ^[hH][tT][tT][pP][sS]?://.+
     */
  mapUrl?: string | null;
  /**
     * @minLength 1
     * @maxLength 100
     */
  name: string;
  /**
     * @minLength 0
     * @maxLength 500
     * @nullable
     * @pattern ^[hH][tT][tT][pP][sS]?://.+
     */
  naverBlogUrl?: string | null;
  /**
     * @minLength 0
     * @maxLength 500
     * @nullable
     * @pattern ^[hH][tT][tT][pP][sS]?://.+
     */
  naverTalkUrl?: string | null;
  /**
     * @minLength 0
     * @maxLength 1000
     * @nullable
     */
  parkingInfo?: string | null;
  /**
     * @minLength 0
     * @maxLength 30
     * @nullable
     */
  phone?: string | null;
  /**
     * @minLength 0
     * @maxLength 20
     * @nullable
     */
  postalCode?: string | null;
  /**
     * @minLength 0
     * @maxLength 100
     * @nullable
     */
  representativeName?: string | null;
  /**
     * @minLength 0
     * @maxLength 500
     * @nullable
     * @pattern ^[hH][tT][tT][pP][sS]?://.+
     */
  smartStoreUrl?: string | null;
}

export const getGetAdminWorkshopProfileUrl = () => {




  return `/api/v1/admin/workshop`
}

export const getAdminWorkshopProfile = async ( options?: RequestInit): Promise<WorkshopProfileResponse> => {

  return generatedApiClient<WorkshopProfileResponse>(getGetAdminWorkshopProfileUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getUpdateAdminWorkshopProfileUrl = () => {




  return `/api/v1/admin/workshop`
}

export const updateAdminWorkshopProfile = async (updateWorkshopProfileRequest: UpdateWorkshopProfileRequest, options?: RequestInit): Promise<WorkshopProfileResponse> => {

  return generatedApiClient<WorkshopProfileResponse>(getUpdateAdminWorkshopProfileUrl(),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateWorkshopProfileRequest)
  }
);}



export const getGetWorkshopProfileUrl = () => {




  return `/api/v1/workshop`
}

export const getWorkshopProfile = async ( options?: RequestInit): Promise<WorkshopProfileResponse> => {

  return generatedApiClient<WorkshopProfileResponse>(getGetWorkshopProfileUrl(),
  {
    ...options,
    method: 'GET'


  }
);}
