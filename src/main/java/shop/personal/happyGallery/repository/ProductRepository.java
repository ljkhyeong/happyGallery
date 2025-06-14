package shop.personal.happyGallery.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.personal.happyGallery.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
