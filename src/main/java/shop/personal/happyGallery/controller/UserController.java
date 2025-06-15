package shop.personal.happyGallery.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import shop.personal.happyGallery.dto.UserResponseDto;
import shop.personal.happyGallery.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	@GetMapping("/{id}")
	public ResponseEntity<UserResponseDto> findUser(@PathVariable Long id) {
		return ResponseEntity.ok()
			.body(userService.findUser(id));
	}

}
