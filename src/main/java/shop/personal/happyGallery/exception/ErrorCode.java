package shop.personal.happyGallery.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode {

	INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "유효하지 않은 인자입니다."),
	INVALID_OPERATION(HttpStatus.BAD_REQUEST, "유효하지 않은 동작입니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
