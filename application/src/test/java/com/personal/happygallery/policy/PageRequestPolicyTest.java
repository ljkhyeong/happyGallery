package com.personal.happygallery.policy;

import com.personal.happygallery.application.shared.page.PageParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.personal.happygallery.policy.PolicyAssertions.assertInvalidInput;

@Tag("policy")
class PageRequestPolicyTest {

    @Test
    @DisplayName("페이지 요청은 음수 번호와 허용 범위 밖 크기 및 OFFSET 오버플로를 거절한다")
    void rejectInvalidPageRequest() {
        assertInvalidInput(() -> PageParams.offset(-1, 20));
        assertInvalidInput(() -> PageParams.requireSize(0));
        assertInvalidInput(() -> PageParams.requireSize(101));
        assertInvalidInput(() -> PageParams.offset(Integer.MAX_VALUE, 100));
    }
}
