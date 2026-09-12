package com.personal.happygallery.adapter.out.external.holiday;

import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DataPortalPublicHolidayProviderTest {

    @DisplayName("공휴일만 변환하고 같은 날짜의 이름은 합친다")
    @Test
    void fetch_mapsOfficialHolidayXml() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        PublicHolidayApiProperties properties = properties();
        DataPortalPublicHolidayProvider provider = new DataPortalPublicHolidayProvider(
                properties,
                builder.baseUrl(properties.baseUrl()).build());
        server.expect(requestTo(allOf(
                        containsString("/SpcdeInfoService/getRestDeInfo"),
                        containsString("ServiceKey=test%2Bkey%2F%3D"),
                        containsString("solYear=2026"))))
                .andRespond(withSuccess("""
                        <response>
                          <header><resultCode>00</resultCode><resultMsg>OK</resultMsg></header>
                          <body>
                            <items>
                              <item><dateName>신정</dateName><locdate>20260101</locdate><isHoliday>Y</isHoliday></item>
                              <item><dateName>광복절</dateName><locdate>20260815</locdate><isHoliday>Y</isHoliday></item>
                              <item><dateName>임시 지정일</dateName><locdate>20260815</locdate><isHoliday>Y</isHoliday></item>
                              <item><dateName>일반 기념일</dateName><locdate>20260901</locdate><isHoliday>N</isHoliday></item>
                            </items>
                            <totalCount>4</totalCount>
                          </body>
                        </response>
                        """, MediaType.APPLICATION_XML));

        var result = provider.fetch(2026).orElseThrow();

        assertThat(result).extracting(holiday -> holiday.date())
                .containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 15));
        assertThat(result.getLast().name()).isEqualTo("광복절 · 임시 지정일");
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({"2,20260101", "0,20260101", "1,20270101"})
    @DisplayName("건수가 불일치하거나 다른 연도인 응답은 연간 목록 교체에 사용하지 않는다")
    void fetch_rejectsIncompleteOrWrongYearResponse(int totalCount, String date) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DataPortalPublicHolidayProvider provider = new DataPortalPublicHolidayProvider(
                properties(), builder.baseUrl(properties().baseUrl()).build());
        server.expect(requestTo(containsString("solYear=2026")))
                .andRespond(withSuccess("""
                        <response><header><resultCode>00</resultCode></header><body>
                          <items><item><dateName>신정</dateName><locdate>%s</locdate><isHoliday>Y</isHoliday></item></items>
                          <totalCount>%d</totalCount>
                        </body></response>
                        """.formatted(date, totalCount), MediaType.APPLICATION_XML));

        assertThat(provider.fetch(2026)).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("명세의 선택 필드가 없는 공휴일 응답도 처리한다")
    void fetch_acceptsMissingOptionalFields() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DataPortalPublicHolidayProvider provider = new DataPortalPublicHolidayProvider(
                properties(), builder.baseUrl(properties().baseUrl()).build());
        server.expect(requestTo(containsString("solYear=2026")))
                .andRespond(withSuccess("""
                        <response><header><resultCode>00</resultCode></header><body><items>
                          <item><dateName> 신정 </dateName><locdate> 20260101 </locdate></item>
                        </items></body></response>
                        """, MediaType.APPLICATION_XML));

        var result = provider.fetch(2026).orElseThrow();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().date()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(result.getFirst().name()).isEqualTo("신정");
        server.verify();
    }

    private static PublicHolidayApiProperties properties() {
        return new PublicHolidayApiProperties(
                true,
                "test+key/=",
                "https://apis.data.go.kr",
                Duration.ofSeconds(5),
                Duration.ofSeconds(1),
                Duration.ofMillis(500),
                5,
                Duration.ofSeconds(30));
    }
}
