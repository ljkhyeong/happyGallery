package shop.personal.happyGallery.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import shop.personal.happyGallery.domain.User;
import shop.personal.happyGallery.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	UserRepository userRepository;


	void test() {
			when(userRepository.getById(1L)).thenReturn(User.builder().id(1L).build());
		verify(userRepository, times(1)).delete(any(User.class));

		verify(userRepository, times(1)).delete(any(User.class));

	}
}
