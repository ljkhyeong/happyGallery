package shop.personal.happyGallery.model;

import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.personal.happyGallery.model.embeded.Address;
import shop.personal.happyGallery.model.embeded.BaseTimeEntity;
import shop.personal.happyGallery.model.embeded.PhoneNumber;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder
@Table(indexes = @Index(columnList = "email", unique = true))
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String email;
	@Column(nullable = false, length = 60)
	private String passwordHash;

	@Embedded
	private Address address;
	@Embedded
	private PhoneNumber phoneNumber;

	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private Cart cart;

	public void connectCart(Cart cart) {
		this.cart = cart;
		cart.setUser(this);
	}

	public void changePassword(PasswordEncoder encoder, String rawPassword) {
		this.passwordHash = encoder.encode(rawPassword);
	}

	public void changeAddress(Address address) {
		this.address = address;
	}

}
