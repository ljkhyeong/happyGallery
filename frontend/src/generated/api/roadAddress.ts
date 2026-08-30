import { generatedApiClient } from '../../shared/api/generatedClient';
export interface RoadAddressResponse {
  buildingName: string;
  jibunAddress: string;
  postalCode: string;
  roadAddress: string;
}

export type SearchRoadAddressesParams = {
/**
 * @minLength 2
 * @maxLength 100
 */
keyword: string;
};

export const getSearchRoadAddressesUrl = (params: SearchRoadAddressesParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/addresses/search?${stringifiedParams}` : `/api/v1/addresses/search`
}

export const searchRoadAddresses = async (params: SearchRoadAddressesParams, options?: RequestInit): Promise<RoadAddressResponse[]> => {

  return generatedApiClient<RoadAddressResponse[]>(getSearchRoadAddressesUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}
