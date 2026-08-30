import { generatedApiClient } from '../../shared/api/generatedClient';
export interface NoticeListResponse {
  createdAt: string;
  id: number;
  pinned: boolean;
  title: string;
  version: number;
  viewCount: number;
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

export const getListPublicNoticesUrl = () => {




  return `/api/v1/notices`
}

export const listPublicNotices = async ( options?: RequestInit): Promise<NoticeListResponse[]> => {

  return generatedApiClient<NoticeListResponse[]>(getListPublicNoticesUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetPublicNoticeUrl = (id: number,) => {




  return `/api/v1/notices/${id}`
}

export const getPublicNotice = async (id: number, options?: RequestInit): Promise<NoticeDetailResponse> => {

  return generatedApiClient<NoticeDetailResponse>(getGetPublicNoticeUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}
