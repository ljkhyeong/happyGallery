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
public class PhoneNumber {
	@Column(name = "phone_country_code")
	private String countryCode;
	@Column(name = "phone_number")
	private String number;
}
