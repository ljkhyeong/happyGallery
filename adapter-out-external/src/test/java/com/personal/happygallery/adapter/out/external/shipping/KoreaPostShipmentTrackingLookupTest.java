package com.personal.happygallery.adapter.out.external.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.personal.happygallery.domain.order.ShipmentTrackingStatus;
import com.personal.happygallery.domain.order.ShippingCarrier;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KoreaPostShipmentTrackingLookupTest {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    @ParameterizedTest
    @CsvSource({"접수,PICKED_UP", "발송,IN_TRANSIT", "도착,IN_TRANSIT",
            "배달준비,OUT_FOR_DELIVERY", "배달완료,DELIVERED", "새로운 상태,UNKNOWN"})
    @DisplayName("우체국 상태를 변환하고 인증키를 인코딩하며 수취인 정보는 제외한다")
    void mapsOfficialResponse(String statusText, ShipmentTrackingStatus expected) {
        server.expect(requestTo(allOf(containsString(KoreaPostShipmentTrackingLookup.PATH),
                        containsString("serviceKey=test%2Bkey%2F%3D"), containsString("rgist=1111111111111"))))
                .andRespond(withSuccess(response(statusText), MediaType.APPLICATION_XML));

        var update = lookup(true).lookup(10L, "11111-1111-1111").orElseThrow();

        assertThat(update.carrier()).isEqualTo(ShippingCarrier.KOREA_POST);
        assertThat(update.trackingNumber()).isEqualTo("11111-1111-1111");
        assertThat(update.status()).isEqualTo(expected);
        assertThat(update.events()).hasSize(2);
        assertThat(update.events().getFirst().occurredAt()).isEqualTo(LocalDateTime.of(2026, 9, 11, 9, 0));
        assertThat(update.events().getLast().location()).isEqualTo("시흥우체국");
        assertThat(update.events()).allSatisfy(event -> assertThat(event.description()).isNull());
        assertThat(update.toString()).doesNotContain("홍길동", "수령인");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<LongitudinalDomesticListResponse><cmmMsgHeader><successYN>N</successYN><returnCode>01</returnCode><errMsg>비밀 오류</errMsg></cmmMsgHeader></LongitudinalDomesticListResponse>",
            "<!DOCTYPE x [<!ENTITY secret SYSTEM 'file:///etc/passwd'>]><x>&secret;</x>",
            "<invalid>"
    })
    @DisplayName("API 오류·외부 엔티티·잘못된 XML은 배송 갱신 결과로 전달하지 않는다")
    void rejectsInvalidResponse(String xml) {
        server.expect(requestTo(containsString(KoreaPostShipmentTrackingLookup.PATH)))
                .andRespond(withSuccess(xml, MediaType.APPLICATION_XML));

        assertThatThrownBy(() -> lookup(true).lookup(10L, "1111111111111"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("우체국 배송조회 응답을 처리하지 못했습니다.")
                .hasNoCause();
        server.verify();
    }

    @Test
    @DisplayName("배송 이력이 없는 정상 응답은 기존 이력을 지우지 않도록 건너뛴다")
    void skipsEmptyHistory() {
        server.expect(requestTo(containsString(KoreaPostShipmentTrackingLookup.PATH)))
                .andRespond(withSuccess("""
                        <LongitudinalDomesticListResponse><cmmMsgHeader>
                        <successYN>Y</successYN><returnCode>00</returnCode>
                        </cmmMsgHeader></LongitudinalDomesticListResponse>
                        """, MediaType.APPLICATION_XML));
        assertThat(lookup(true).lookup(10L, "1111111111111")).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("비활성 연동과 잘못된 운송장은 외부 API를 호출하지 않는다")
    void avoidsUnnecessaryRequests() {
        assertThat(lookup(false).lookup(10L, "1111111111111")).isEmpty();
        assertThatThrownBy(() -> lookup(true).lookup(10L, "123"))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @Test
    @DisplayName("기본 설정은 비활성이며 활성 설정은 서비스키를 요구한다")
    void validatesEnabledConfiguration() {
        var context = new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);
        context.run(result -> assertThat(result.getBean(KoreaPostProperties.class).enabled()).isFalse());
        context.withPropertyValues("app.external.korea-post.enabled=true")
                .run(result -> assertThat(result).hasFailed());
        context.withPropertyValues("app.external.korea-post.enabled=true", "app.external.korea-post.service-key=test")
                .run(result -> assertThat(result).hasNotFailed());
    }

    private KoreaPostShipmentTrackingLookup lookup(boolean enabled) {
        return new KoreaPostShipmentTrackingLookup(new KoreaPostProperties(enabled, "test+key/=",
                Duration.ofSeconds(5), Duration.ofSeconds(1), Duration.ofMillis(500), 5, Duration.ofSeconds(30)),
                builder.baseUrl("http://openapi.epost.go.kr").build());
    }

    private static String response(String status) {
        return """
                <LongitudinalDomesticListResponse>
                  <cmmMsgHeader><successYN>Y</successYN><returnCode>00</returnCode></cmmMsgHeader>
                  <addrseNm>홍길동</addrseNm><dlvySttus>%s</dlvySttus>
                  <longitudinalDomesticList><dlvyDate>2026-09-12</dlvyDate><dlvyTime>16:26</dlvyTime>
                    <nowLc>시흥우체국</nowLc><processSttus>%s</processSttus><detailDc>수령인:홍길동</detailDc>
                  </longitudinalDomesticList>
                  <longitudinalDomesticList><dlvyDate>2026.09.11</dlvyDate><dlvyTime>09:00</dlvyTime>
                    <nowLc>서울우체국</nowLc><processSttus>접수</processSttus>
                  </longitudinalDomesticList>
                </LongitudinalDomesticListResponse>
                """.formatted(status, status);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(KoreaPostProperties.class)
    static class PropertiesConfig {}
}
