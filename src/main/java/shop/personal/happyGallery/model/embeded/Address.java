package shop.personal.happyGallery.model.embeded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class Address {
	private String city;
	private String zipCode;
	private String street;
	@Column(name = "address_etc")
	private String etc;

}
