import { generatedApiClient } from '../../shared/api/generatedClient';
export interface AdminQnaResponse {
  authorName: string;
  content: string;
  createdAt: string;
  id: number;
  productId: number;
  /** @nullable */
  repliedAt: string | null;
  /** @nullable */
  replyContent: string | null;
  secret: boolean;
  title: string;
  userId: number;
}

export interface AdminQnaPageResponse {
  content: AdminQnaResponse[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
}

export interface QnaReplyRequest {
  /**
     * @minLength 1
     * @maxLength 16000
     */
  replyContent: string;
}

export interface SmartStoreInquiryResponse {
  /** @nullable */
  answer: string | null;
  answered: boolean;
  channelProductId: number;
  createdAt: string;
  maskedWriterId: string;
  productName: string;
  question: string;
  questionId: number;
}

export interface SmartStoreInquiryAnswerRequest {
  /**
     * @minLength 1
     * @maxLength 16000
     */
  content: string;
}

export interface MyProductQnaListItem {
  createdAt: string;
  hasReply: boolean;
  id: number;
  secret: boolean;
  title: string;
}

export interface CreateQnaRequest {
  /**
     * @minLength 1
     * @maxLength 16000
     */
  content: string;
  secret: boolean;
  /**
     * @minLength 1
     * @maxLength 200
     */
  title: string;
}

export interface QnaCreatedResponse {
  createdAt: string;
  id: number;
  productId: number;
  secret: boolean;
  title: string;
}

export interface MyProductQnaPageResponse {
  content: MyProductQnaListItem[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
}

export interface ProductQnaDetail {
  authorName: string;
  content: string;
  createdAt: string;
  id: number;
  productId: number;
  /** @nullable */
  repliedAt: string | null;
  /** @nullable */
  replyContent: string | null;
  secret: boolean;
  title: string;
}

export interface ProductQnaListItem {
  authorName: string;
  createdAt: string;
  hasReply: boolean;
  id: number;
  secret: boolean;
  title: string;
}

export interface ProductQnaPageResponse {
  content: ProductQnaListItem[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
}

export type ListAdminProductQnaParams = {
productId: number;
};

export type ListAdminProductQnaPageParams = {
productId: number;
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
};

export type ListUnansweredAdminProductQnaParams = {
cursor?: string;
size?: number;
};

export type ListSmartStoreInquiriesParams = {
unansweredOnly?: boolean;
/**
 * @minimum 1
 * @maximum 200
 */
limit?: number;
};

export type ListMyProductQnaPageParams = {
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
};

export type ListProductQnaPageParams = {
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
};

export const getListAdminProductQnaUrl = (params: ListAdminProductQnaParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/qna?${stringifiedParams}` : `/api/v1/admin/qna`
}

export const listAdminProductQna = async (params: ListAdminProductQnaParams, options?: RequestInit): Promise<AdminQnaResponse[]> => {

  return generatedApiClient<AdminQnaResponse[]>(getListAdminProductQnaUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListAdminProductQnaPageUrl = (params: ListAdminProductQnaPageParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/qna/page?${stringifiedParams}` : `/api/v1/admin/qna/page`
}

export const listAdminProductQnaPage = async (params: ListAdminProductQnaPageParams, options?: RequestInit): Promise<AdminQnaPageResponse> => {

  return generatedApiClient<AdminQnaPageResponse>(getListAdminProductQnaPageUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListUnansweredAdminProductQnaUrl = (params?: ListUnansweredAdminProductQnaParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/qna/unanswered?${stringifiedParams}` : `/api/v1/admin/qna/unanswered`
}

export const listUnansweredAdminProductQna = async (params?: ListUnansweredAdminProductQnaParams, options?: RequestInit): Promise<AdminQnaPageResponse> => {

  return generatedApiClient<AdminQnaPageResponse>(getListUnansweredAdminProductQnaUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getReplyProductQnaUrl = (id: number,) => {




  return `/api/v1/admin/qna/${id}/reply`
}

export const replyProductQna = async (id: number,
    qnaReplyRequest: QnaReplyRequest, options?: RequestInit): Promise<AdminQnaResponse> => {

  return generatedApiClient<AdminQnaResponse>(getReplyProductQnaUrl(id),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(qnaReplyRequest)
  }
);}



export const getListSmartStoreInquiriesUrl = (params?: ListSmartStoreInquiriesParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/smartstore-inquiries?${stringifiedParams}` : `/api/v1/admin/smartstore-inquiries`
}

export const listSmartStoreInquiries = async (params?: ListSmartStoreInquiriesParams, options?: RequestInit): Promise<SmartStoreInquiryResponse[]> => {

  return generatedApiClient<SmartStoreInquiryResponse[]>(getListSmartStoreInquiriesUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getAnswerSmartStoreInquiryUrl = (questionId: number,) => {




  return `/api/v1/admin/smartstore-inquiries/${questionId}/answer`
}

export const answerSmartStoreInquiry = async (questionId: number,
    smartStoreInquiryAnswerRequest: SmartStoreInquiryAnswerRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getAnswerSmartStoreInquiryUrl(questionId),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(smartStoreInquiryAnswerRequest)
  }
);}



export const getListMyProductQnaUrl = (productId: number,) => {




  return `/api/v1/me/products/${productId}/qna`
}

export const listMyProductQna = async (productId: number, options?: RequestInit): Promise<MyProductQnaListItem[]> => {

  return generatedApiClient<MyProductQnaListItem[]>(getListMyProductQnaUrl(productId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCreateProductQnaUrl = (productId: number,) => {




  return `/api/v1/me/products/${productId}/qna`
}

export const createProductQna = async (productId: number,
    createQnaRequest: CreateQnaRequest, options?: RequestInit): Promise<QnaCreatedResponse> => {

  return generatedApiClient<QnaCreatedResponse>(getCreateProductQnaUrl(productId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(createQnaRequest)
  }
);}



export const getListMyProductQnaPageUrl = (productId: number,
    params?: ListMyProductQnaPageParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/me/products/${productId}/qna/page?${stringifiedParams}` : `/api/v1/me/products/${productId}/qna/page`
}

export const listMyProductQnaPage = async (productId: number,
    params?: ListMyProductQnaPageParams, options?: RequestInit): Promise<MyProductQnaPageResponse> => {

  return generatedApiClient<MyProductQnaPageResponse>(getListMyProductQnaPageUrl(productId,params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetMyProductQnaUrl = (productId: number,
    id: number,) => {




  return `/api/v1/me/products/${productId}/qna/${id}`
}

export const getMyProductQna = async (productId: number,
    id: number, options?: RequestInit): Promise<ProductQnaDetail> => {

  return generatedApiClient<ProductQnaDetail>(getGetMyProductQnaUrl(productId,id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListProductQnaUrl = (productId: number,) => {




  return `/api/v1/products/${productId}/qna`
}

export const listProductQna = async (productId: number, options?: RequestInit): Promise<ProductQnaListItem[]> => {

  return generatedApiClient<ProductQnaListItem[]>(getListProductQnaUrl(productId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListProductQnaPageUrl = (productId: number,
    params?: ListProductQnaPageParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/products/${productId}/qna/page?${stringifiedParams}` : `/api/v1/products/${productId}/qna/page`
}

export const listProductQnaPage = async (productId: number,
    params?: ListProductQnaPageParams, options?: RequestInit): Promise<ProductQnaPageResponse> => {

  return generatedApiClient<ProductQnaPageResponse>(getListProductQnaPageUrl(productId,params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetPublicProductQnaUrl = (productId: number,
    id: number,) => {




  return `/api/v1/products/${productId}/qna/${id}`
}

export const getPublicProductQna = async (productId: number,
    id: number, options?: RequestInit): Promise<ProductQnaDetail> => {

  return generatedApiClient<ProductQnaDetail>(getGetPublicProductQnaUrl(productId,id),
  {
    ...options,
    method: 'GET'


  }
);}
