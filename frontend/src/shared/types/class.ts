export interface ClassResponse {
  id: number;
  name: string;
  category: string;
  durationMin: number;
  price: number;
  bufferMin: number;
  passEligible: boolean;
  description: string | null;
  imageUrl: string | null;
  preparationInfo: string | null;
  targetAudience: string | null;
  status: ClassStatus;
}

export type ClassStatus = "ACTIVE" | "INACTIVE";

export interface CreateClassRequest {
  name: string;
  category: string;
  durationMin: number;
  price: number;
  bufferMin: number;
  passEligible: boolean;
  description?: string;
  imageUrl?: string;
  preparationInfo?: string;
  targetAudience?: string;
}

export interface UpdateClassRequest {
  name: string;
  category: string;
  price: number;
  passEligible: boolean;
  description?: string;
  imageUrl?: string;
  preparationInfo?: string;
  targetAudience?: string;
}
