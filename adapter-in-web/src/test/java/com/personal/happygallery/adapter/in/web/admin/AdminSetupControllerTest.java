package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminSetupRequest;
import com.personal.happygallery.adapter.in.web.config.properties.AdminSetupProperties;
import com.personal.happygallery.application.admin.port.in.AdminSetupUseCase;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSetupControllerTest {

    @DisplayName("setup 이 활성 상태이고 관리자 계정이 없으면 status 는 required=true 를 반환한다")
    @Test
    void status_returnsRequiredTrue_whenSetupEnabledAndAvailable() {
        AdminSetupUseCase useCase = Mockito.mock(AdminSetupUseCase.class);
        when(useCase.isAvailable()).thenReturn(true);
        AdminSetupController controller = new AdminSetupController(new AdminSetupProperties("setup-token"), useCase);

        assertThat(controller.status().required()).isTrue();
    }

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

    @DisplayName("setup 이 활성 상태이고 토큰이 일치하면 최초 관리자 생성을 위임한다")
    @Test
    void setup_callsUseCase_whenSetupEnabledAndTokenMatches() {
        AdminSetupUseCase useCase = Mockito.mock(AdminSetupUseCase.class);
        when(useCase.isAvailable()).thenReturn(true);
        AdminSetupController controller = new AdminSetupController(new AdminSetupProperties("setup-token"), useCase);

        controller.setup(new AdminSetupRequest("setup-token", "admin", "admin123456"));

        verify(useCase).setup("admin", "admin123456");
    }
}
