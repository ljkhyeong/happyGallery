package shop.personal.happyGallery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import shop.personal.happyGallery.model.User;

@AllArgsConstructor
@Getter
public class UserResponseDto {

	private final String email;
	private final String address;
	public static UserResponseDto from(User user) {
		return new UserResponseDto(user.getEmail(), user.getAddress());
	}
}
