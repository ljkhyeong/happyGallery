package com.personal.happygallery.adapter.in.web.monitoring;

import com.personal.happygallery.adapter.in.web.GlobalExceptionHandler;
import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.ISpan;
import io.sentry.ITransportFactory;
import io.sentry.Sentry;
import io.sentry.SentryEnvelope;
import io.sentry.SentryItemType;
import io.sentry.SentryOptions;
import io.sentry.spring.boot4.SentryAutoConfiguration;
import io.sentry.transport.ITransport;
import io.sentry.transport.RateLimiter;
import io.sentry.util.UrlUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.AbstractFilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SentryEventSanitizerTest {

    private static final String ACCESS_TOKEN = "guest-access-token-raw";
    private static final String PAYMENT_STATUS_TOKEN = "payment-status-token-raw";
    private static final String BOT_TOKEN = "turnstile-bot-token-raw";
    private static final String BEARER_TOKEN = "admin-bearer-token-raw";
    private static final String SESSION_ID = "member-session-raw";
    private static final String SEARCH_KEYWORD = "01055551234";
    private static final String PAYMENT_KEY = "toss-payment-key-raw";
    private static final String SERVICE_KEY = "korea-post-service-key-raw";
    private static final String USER_AGENT = "happygallery-sentry-test";
    private static final String EXTERNAL_URL = "https://external.example/track";

    private final List<SentryEnvelope> envelopes = new CopyOnWriteArrayList<>();
    private final AtomicReference<SentryOptions> sentryOptions = new AtomicReference<>();
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SentryAutoConfiguration.class))
            .withBean(SentryEventSanitizer.class)
            .withBean(ITransportFactory.class, () -> (options, requestDetails) -> {
                sentryOptions.set(options);
                return new CapturingTransport(envelopes);
            })
            .withPropertyValues(
                    "sentry.dsn=https://public@sentry.invalid/1",
                    "sentry.send-default-pii=false",
                    "sentry.traces-sample-rate=1.0");

    @AfterEach
    void closeSentry() {
        Sentry.close();
    }

    @DisplayName("서버 오류 이벤트와 트랜잭션은 비회원 token 헤더와 요청·Referer·외부 호출 query를 전송하지 않는다")
    @Test
    void capturedEvents_excludeSensitiveRequestData() {
        contextRunner.run(context -> {
            MockMvc mockMvc = standaloneSetup(new FailingOrderController())
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .addFilters(sentryFilters(context))
                    .build();

            mockMvc.perform(get("/api/v1/orders/{id}", 1)
                            .queryParam("keyword", SEARCH_KEYWORD)
                            .header("X-Access-Token", ACCESS_TOKEN)
                            .header("X-Payment-Status-Token", PAYMENT_STATUS_TOKEN)
                            .header("X-Bot-Token", BOT_TOKEN)
                            .header("Authorization", "Bearer " + BEARER_TOKEN)
                            .header("Referer", "https://shop.example/payments/success?paymentKey=" + PAYMENT_KEY)
                            .header("User-Agent", USER_AGENT)
                            .cookie(new Cookie("HG_SESSION", SESSION_ID)))
                    .andExpect(status().isInternalServerError());

            String event = serializedEnvelope(SentryItemType.Event);
            String transaction = serializedEnvelope(SentryItemType.Transaction);
            assertSoftly(softly -> {
                for (String payload : List.of(event, transaction)) {
                    softly.assertThat(payload)
                            .contains(USER_AGENT)
                            .doesNotContain(ACCESS_TOKEN, PAYMENT_STATUS_TOKEN, BOT_TOKEN, BEARER_TOKEN,
                                    SESSION_ID, SEARCH_KEYWORD, PAYMENT_KEY, SERVICE_KEY);
                }
                softly.assertThat(event)
                        .contains("http://localhost/api/v1/orders/1")
                        .contains("https://shop.example/payments/success")
                        .contains(EXTERNAL_URL);
                softly.assertThat(transaction).contains("http.client");
            });
        });
    }

    private static Filter[] sentryFilters(ApplicationContext context) {
        return context.getBeansOfType(AbstractFilterRegistrationBean.class).values().stream()
                .sorted(Comparator.comparingInt(AbstractFilterRegistrationBean::getOrder))
                .map(AbstractFilterRegistrationBean::getFilter)
                .toArray(Filter[]::new);
    }

    private String serializedEnvelope(SentryItemType type) throws Exception {
        SentryEnvelope envelope = envelopes.stream()
                .filter(candidate -> hasItem(candidate, type))
                .findFirst()
                .orElseThrow(() -> new AssertionError(type + " envelope가 전송되지 않았습니다."));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        sentryOptions.get().getSerializer().serialize(envelope, output);
        return output.toString(StandardCharsets.UTF_8);
    }

    private static boolean hasItem(SentryEnvelope envelope, SentryItemType type) {
        for (var item : envelope.getItems()) {
            if (item.getHeader().getType() == type) {
                return true;
            }
        }
        return false;
    }

    @RestController
    static class FailingOrderController {

        /** 외부 API 호출 계측(SentrySpanClientHttpRequestInterceptor)과 같은 방식으로 query를 남긴 뒤 실패한다. */
        @GetMapping("/api/v1/orders/{id}")
        String order(@PathVariable Long id) {
            String externalUrl = EXTERNAL_URL + "?serviceKey=" + SERVICE_KEY;
            Sentry.addBreadcrumb(Breadcrumb.http(externalUrl, "GET"));
            ISpan span = Sentry.getSpan().startChild("http.client", "GET " + EXTERNAL_URL);
            UrlUtils.parse(externalUrl).applyToSpan(span);
            span.finish();
            throw new IllegalStateException("주문 조회 실패");
        }
    }

    private record CapturingTransport(List<SentryEnvelope> envelopes) implements ITransport {

        @Override
        public void send(SentryEnvelope envelope, Hint hint) {
            envelopes.add(envelope);
        }

        @Override
        public void flush(long timeoutMillis) {
        }

        @Override
        public RateLimiter getRateLimiter() {
            return null;
        }

        @Override
        public void close(boolean isRestarting) {
        }

        @Override
        public void close() {
        }
    }
}
