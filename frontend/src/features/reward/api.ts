import {
  getMyRewardWallet,
  type RewardWalletResponse,
} from "@/generated/api/memberBenefit";

export type {
  RewardHistoryResponse,
  RewardWalletResponse,
} from "@/generated/api/memberBenefit";

export function fetchMyRewardWallet(signal?: AbortSignal): Promise<RewardWalletResponse> {
  return getMyRewardWallet({ signal });
}
