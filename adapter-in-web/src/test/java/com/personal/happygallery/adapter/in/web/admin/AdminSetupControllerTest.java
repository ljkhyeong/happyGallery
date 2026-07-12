package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminSetupRequest;
import com.personal.happygallery.adapter.in.web.config.properties.AdminSetupProperties;
import com.personal.happygallery.application.admin.port.in.AdminSetupUseCase;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSetupControllerTest {

    @DisplayName("setup 토큰이 다르면 401 예외를 던지고 계정을 만들지 않는다")
    @Test
    void setup_throwsUnauthorized_whenTokenDoesNotMatch() {
        AdminSetupUseCase useCase = Mockito.mock(AdminSetupUseCase.class);
        when(useCase.isAvailable()).thenReturn(true);
        AdminSetupController controller = new AdminSetupController(new AdminSetupProperties("setup-token"), useCase);

        assertThatThrownBy(() -> controller.setup(new AdminSetupRequest("wrong-token", "admin", "admin123456")))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessage("setup 토큰이 일치하지 않습니다.");
        verify(useCase, never()).setup(Mockito.anyString(), Mockito.anyString());
    }
}
