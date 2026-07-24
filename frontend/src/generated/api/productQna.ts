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

export interface QnaReplyRequest {
  /** @minLength 1 */
  replyContent: string;
}

export interface CreateQnaRequest {
  /** @minLength 1 */
  content: string;
  /**
     * @minLength 4
     * @maxLength 20
     */
  password?: string;
  secret?: boolean;
  /**
     * @minLength 0
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

export interface ProductQnaListItem {
  authorName: string;
  createdAt: string;
  hasReply: boolean;
  id: number;
  secret: boolean;
  title: string;
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

export interface VerifyQnaPasswordRequest {
  /** @minLength 1 */
  password: string;
}

export type ListAdminProductQnaParams = {
productId: number;
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



export const getVerifyProductQnaPasswordUrl = (productId: number,
    id: number,) => {




  return `/api/v1/products/${productId}/qna/${id}/verify`
}

export const verifyProductQnaPassword = async (productId: number,
    id: number,
    verifyQnaPasswordRequest: VerifyQnaPasswordRequest, options?: RequestInit): Promise<ProductQnaDetail> => {

  return generatedApiClient<ProductQnaDetail>(getVerifyProductQnaPasswordUrl(productId,id),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(verifyQnaPasswordRequest)
  }
);}
