package shop.personal.happyGallery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import shop.personal.happyGallery.domain.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

}
