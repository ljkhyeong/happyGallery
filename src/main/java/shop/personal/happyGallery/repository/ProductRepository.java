package shop.personal.happyGallery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import shop.personal.happyGallery.domain.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
