export interface WorkshopProfile {
  name: string;
  phone: string | null;
  postalCode: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  businessHours: string | null;
  mapUrl: string | null;
  parkingInfo: string | null;
  businessRegistrationNumber: string | null;
  representativeName: string | null;
  email: string | null;
  mailOrderRegistrationNumber: string | null;
  introduction: string | null;
  kakaoTalkId: string | null;
  naverTalkEnabled: boolean;
  updatedAt: string;
}

export type UpdateWorkshopProfileRequest = Omit<WorkshopProfile, "updatedAt">;
