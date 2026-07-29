import {
  registerMyVerifiedEmail,
  sendMyEmailVerification,
} from "@/generated/api/customerAuth";

export function sendEmailVerification(email: string) {
  return sendMyEmailVerification({ email });
}

export function registerVerifiedEmail(email: string, verificationCode: string) {
  return registerMyVerifiedEmail({ email, verificationCode });
}
