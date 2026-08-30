package com.personal.happygallery.adapter.out.external.holiday;

import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DataPortalPublicHolidayProviderTest {

    @DisplayName("공공데이터 특일 XML 응답을 연도별 공휴일로 변환한다")
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
                        containsString("solYear=2026"))))
                .andRespond(withSuccess("""
                        <response>
                          <header><resultCode>00</resultCode><resultMsg>OK</resultMsg></header>
                          <body>
                            <items>
                              <item><dateName>신정</dateName><locdate>20260101</locdate></item>
                              <item><dateName>광복절</dateName><locdate>20260815</locdate></item>
                              <item><dateName>임시 지정일</dateName><locdate>20260815</locdate></item>
                            </items>
                          </body>
                        </response>
                        """, MediaType.APPLICATION_XML));

        var result = provider.fetch(2026).orElseThrow();

        assertThat(result).extracting(holiday -> holiday.date())
                .containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 15));
        assertThat(result.getLast().name()).isEqualTo("광복절 · 임시 지정일");
        server.verify();
    }

    private static PublicHolidayApiProperties properties() {
        return new PublicHolidayApiProperties(
                true,
                "service-key",
                "https://apis.data.go.kr",
                Duration.ofSeconds(5),
                Duration.ofSeconds(1),
                Duration.ofMillis(500),
                5,
                Duration.ofSeconds(30));
    }
}
