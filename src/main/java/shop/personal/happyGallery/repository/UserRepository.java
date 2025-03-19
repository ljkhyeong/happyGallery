package shop.personal.happyGallery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import shop.personal.happyGallery.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
