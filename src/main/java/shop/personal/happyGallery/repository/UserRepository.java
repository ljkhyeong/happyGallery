package shop.personal.happyGallery.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import shop.personal.happyGallery.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
