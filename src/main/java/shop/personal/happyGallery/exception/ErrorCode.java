package shop.personal.happyGallery.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode {

	NOT_NEGATIVE_CARTITEM_QUANTITY(HttpStatus.BAD_REQUEST, "장바구니 아이템 추가는 1개 이상 해야합니다."),
	NOT_NEGATIVE_STOCK(HttpStatus.BAD_REQUEST, "재고 변경은 1개 이상이어야 합니다."),
	NOT_DECREASE_OVER_STOCK(HttpStatus.BAD_REQUEST, "재고감소수량은 재고 이상일 수 없습니다."),
	NOT_OVER_THOUSAND_CARTITEM_QUANTITY(HttpStatus.BAD_REQUEST, "장바구니 아이템 추가는 1000개 이하만 가능합니다."),
	NOT_OVER_THOUSAND_STOCK(HttpStatus.BAD_REQUEST, "재고는 1000개까지만 등록이 가능합니다."),
	NOT_EXISTS_ITEM_IN_CART(HttpStatus.BAD_REQUEST, "장바구니 안에 해당 아이템이 존재하지 않습니다."),
	EMPTY_CART(HttpStatus.BAD_REQUEST, "장바구니가 비어있습니다."),
	INVALID_OPERATION_ORDERSTATUS(HttpStatus.BAD_REQUEST, "현재 주문상태에서는 할 수 없는 주문상태 변경입니다."),
	INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "유효하지 않은 인자입니다."),
	INVALID_OPERATION(HttpStatus.BAD_REQUEST, "유효하지 않은 동작입니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
