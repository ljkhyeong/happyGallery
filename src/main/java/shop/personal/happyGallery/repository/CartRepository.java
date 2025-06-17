package shop.personal.happyGallery.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.personal.happyGallery.model.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
	Optional<Cart> findByUserId(Long userId);
}
