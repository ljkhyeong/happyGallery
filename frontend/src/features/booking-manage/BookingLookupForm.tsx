import { GuestLookupForm } from "@/features/guest-lookup/GuestLookupForm";

interface Props {
  onLookup: (bookingId: number, token: string) => void;
  isLoading: boolean;
  initialBookingId?: string;
  initialToken?: string;
}

export function BookingLookupForm({ onLookup, isLoading, initialBookingId, initialToken }: Props) {
  return <GuestLookupForm kind="bookings" onLookup={onLookup} isLoading={isLoading}
    initialId={initialBookingId} initialToken={initialToken} />;
}
