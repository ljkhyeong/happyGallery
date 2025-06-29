package shop.personal.happyGallery.model;

import javax.naming.OperationNotSupportedException;

import lombok.Getter;
import shop.personal.happyGallery.exception.ApplicationException;
import shop.personal.happyGallery.exception.ErrorCode;

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
		throw cannot();
	}
	public OrderStatus deliver() {
		throw cannot();
	}
	public OrderStatus delivered() {
		throw cannot();
	}
	public OrderStatus complete() {
		throw cannot();
	}
	public OrderStatus refund() {
		throw cannot();
	}

	public OrderStatus refunded() {
		throw cannot();
	}
	public OrderStatus cancel() {
		throw cannot();
	}

	private ApplicationException cannot() {
		return new ApplicationException(ErrorCode.INVALID_OPERATION_ORDERSTATUS);
	}
}
