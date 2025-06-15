package shop.personal.happyGallery.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.personal.happyGallery.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

	List<Product> findByCategoryId(Long id);
}
