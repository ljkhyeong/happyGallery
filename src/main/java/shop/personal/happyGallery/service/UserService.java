package shop.personal.happyGallery.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import shop.personal.happyGallery.dto.UserResponseDto;
import shop.personal.happyGallery.model.User;
import shop.personal.happyGallery.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public UserResponseDto findUser(Long id) {
		User user = userRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저"));

		return UserResponseDto.from(user);
	}
}
