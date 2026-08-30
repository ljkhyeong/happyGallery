import { generatedApiClient } from '../../shared/api/generatedClient';
export interface NoticeListResponse {
  createdAt: string;
  id: number;
  pinned: boolean;
  title: string;
  version: number;
  viewCount: number;
}

export interface CreateNoticeRequest {
  /**
     * @minLength 1
     * @maxLength 16000
     */
  content: string;
  pinned: boolean;
  /**
     * @minLength 1
     * @maxLength 200
     */
  title: string;
}

export interface NoticeDetailResponse {
  content: string;
  createdAt: string;
  id: number;
  pinned: boolean;
  title: string;
  version: number;
  viewCount: number;
}

export interface UpdateNoticeRequest {
  /**
     * @minLength 1
     * @maxLength 16000
     */
  content: string;
  expectedVersion: number;
  pinned: boolean;
  /**
     * @minLength 1
     * @maxLength 200
     */
  title: string;
}

export type DeleteAdminNoticeParams = {
/**
 * @minimum 0
 */
expectedVersion: number;
};

export const getListAdminNoticesUrl = () => {




  return `/api/v1/admin/notices`
}

export const listAdminNotices = async ( options?: RequestInit): Promise<NoticeListResponse[]> => {

  return generatedApiClient<NoticeListResponse[]>(getListAdminNoticesUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCreateAdminNoticeUrl = () => {




  return `/api/v1/admin/notices`
}

export const createAdminNotice = async (createNoticeRequest: CreateNoticeRequest, options?: RequestInit): Promise<NoticeDetailResponse> => {

  return generatedApiClient<NoticeDetailResponse>(getCreateAdminNoticeUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(createNoticeRequest)
  }
);}



export const getDeleteAdminNoticeUrl = (id: number,
    params: DeleteAdminNoticeParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/notices/${id}?${stringifiedParams}` : `/api/v1/admin/notices/${id}`
}

export const deleteAdminNotice = async (id: number,
    params: DeleteAdminNoticeParams, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getDeleteAdminNoticeUrl(id,params),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getGetAdminNoticeUrl = (id: number,) => {




  return `/api/v1/admin/notices/${id}`
}

export const getAdminNotice = async (id: number, options?: RequestInit): Promise<NoticeDetailResponse> => {

  return generatedApiClient<NoticeDetailResponse>(getGetAdminNoticeUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getUpdateAdminNoticeUrl = (id: number,) => {




  return `/api/v1/admin/notices/${id}`
}

export const updateAdminNotice = async (id: number,
    updateNoticeRequest: UpdateNoticeRequest, options?: RequestInit): Promise<NoticeDetailResponse> => {

  return generatedApiClient<NoticeDetailResponse>(getUpdateAdminNoticeUrl(id),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateNoticeRequest)
  }
);}
