package shop.personal.happyGallery.exception;

public class ApplicationException extends RuntimeException {

	public ApplicationException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
	private final ErrorCode errorCode;
}
