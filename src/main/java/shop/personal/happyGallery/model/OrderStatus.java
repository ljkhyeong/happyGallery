package shop.personal.happyGallery.model;

import javax.naming.OperationNotSupportedException;

import lombok.Getter;

public enum OrderStatus {
	// TODO 배송준비중에서도 취소요청은 가능하도록
	PLACED{
		@Override
		public OrderStatus prepareDelivering() {
			return DELIVERING_PREPARING;
		}
		@Override
		public OrderStatus cancel() {
			return CANCELED;
		}
	}, DELIVERING_PREPARING{
		@Override
		public OrderStatus deliver() {
			return DELIVERING;
		}
	}, DELIVERING{
		@Override
		public OrderStatus delivered() {
			return DELIVERED;
		}
	}, DELIVERED{
		@Override
		public OrderStatus complete() {
			return COMPLETED;
		}
	}, COMPLETED{
		@Override
		public OrderStatus refund() {
			return REFUNDING;
		}
	}, REFUNDING{
		@Override
		public OrderStatus refunded() {
			return REFUNDED;
		}
	}, REFUNDED, CANCELED;

	public OrderStatus prepareDelivering() {
		throw new UnsupportedOperationException("현재 배송상태에서는 불가능한 변경입니다.");
	}
	public OrderStatus deliver() {
		throw new UnsupportedOperationException("현재 배송상태에서는 불가능한 변경입니다.");
	}
	public OrderStatus delivered() {
		throw new UnsupportedOperationException("현재 배송상태에서는 불가능한 변경입니다.");
	}
	public OrderStatus complete() {
		throw new UnsupportedOperationException("현재 배송상태에서는 불가능한 변경입니다.");
	}
	public OrderStatus refund() {
		throw new UnsupportedOperationException("현재 배송상태에서는 불가능한 변경입니다.");
	}

	public OrderStatus refunded() {
		throw new UnsupportedOperationException("현재 배송상태에서는 불가능한 변경입니다.");
	}
	public OrderStatus cancel() {
		throw new UnsupportedOperationException("현재 배송상태에서는 불가능한 변경입니다.");
	}

}
