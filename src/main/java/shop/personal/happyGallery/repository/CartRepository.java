package shop.personal.happyGallery.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.personal.happyGallery.model.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
