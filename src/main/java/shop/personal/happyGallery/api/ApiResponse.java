package shop.personal.happyGallery.api;

public record ApiResponse<T> (
	String code,
	T data
) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>("SUCCESS", data);
	}

	public static <Void> ApiResponse<Void> fail() {
		return new ApiResponse<>("FAIL", null);
	}
}
