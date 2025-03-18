package shop.personal.happyGallery.ex;

import static org.assertj.core.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class JUnitTest {
	@Autowired
	ApplicationContext context;

	static Set<JUnitTest> testObject = new HashSet<>();
	static ApplicationContext contextObject = null;
	@Test
	void test1() {
		assertThat(testObject).doesNotContain(this);
		testObject.add(this);

		assertThat(contextObject == null || contextObject == this.context).isTrue();
	}

	@Test
	void test2() {
		assertThat(testObject).doesNotContain(this);
		testObject.add(this);

		assertThat(contextObject == null || contextObject == this.context).isTrue();
	}

	@Test
	void test3() {
		assertThat(testObject).doesNotContain(this);
		testObject.add(this);

		assertThat(contextObject).isIn(null, this.context);
	}
}
