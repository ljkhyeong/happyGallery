package shop.personal.happyGallery.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.personal.happyGallery.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
