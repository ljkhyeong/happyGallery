package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import shop.personal.happyGallery.model.User;
import shop.personal.happyGallery.model.embeded.Address;

public record UserResponseDto(
	String email,
	Address address
) {

	public static UserResponseDto from(User user) {
		return new UserResponseDto(user.getEmail(), user.getAddress());
	}
}
