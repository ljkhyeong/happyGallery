package shop.personal.happyGallery.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.personal.happyGallery.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
