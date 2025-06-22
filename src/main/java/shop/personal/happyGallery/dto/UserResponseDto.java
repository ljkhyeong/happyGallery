package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import shop.personal.happyGallery.model.User;
import shop.personal.happyGallery.model.embeded.Address;

@AllArgsConstructor
@Getter
public class UserResponseDto {

	private final String email;
	private final Address address;
	public static UserResponseDto from(User user) {
		return new UserResponseDto(user.getEmail(), user.getAddress());
	}
}
