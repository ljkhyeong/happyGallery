package com.personal.happygallery.adapter.in.web.monitoring;

import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.SentryBaseEvent;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryTransaction;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 서버 Sentry 전송 직전에 URL query·fragment, 요청 본문·쿠키, 인증·비회원 token 헤더를 제거한다.
 *
 * <p>sentry-spring은 scope의 요청 정보에 query string을 그대로 담아 오류 이벤트와 트랜잭션에 함께 붙이고,
 * SDK 기본 제거 헤더에는 {@code X-Access-Token}·{@code X-Payment-Status-Token}·{@code X-Bot-Token}이 없다.
 * 외부 호출 breadcrumb·span도 query를 {@code http.query}로 남기므로 같은 기준으로 제거한다(ADR-0028).
 */
@Component
public class SentryEventSanitizer
        implements SentryOptions.BeforeSendCallback, SentryOptions.BeforeSendTransactionCallback {

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization",
            "cookie",
            "x-access-token",
            "x-payment-status-token",
            "x-xsrf-token",
            "x-bot-token");
    private static final Set<String> REFERRER_HEADERS = Set.of("referer", "referrer");
    private static final Set<String> URL_PART_DATA_KEYS = Set.of("http.query", "http.fragment");
    private static final Pattern QUERY_OR_FRAGMENT = Pattern.compile("[?#].*", Pattern.DOTALL);

    @Override
    public SentryEvent execute(SentryEvent event, Hint hint) {
        sanitize(event);
        return event;
    }

    @Override
    public SentryTransaction execute(SentryTransaction transaction, Hint hint) {
        sanitize(transaction);
        transaction.getSpans().forEach(span -> span.setData(withoutUrlParts(span.getData())));
        return transaction;
    }

    private static void sanitize(SentryBaseEvent event) {
        Request request = event.getRequest();
        if (request != null) {
            request.setUrl(withoutQuery(request.getUrl()));
            request.setQueryString(null);
            request.setFragment(null);
            request.setCookies(null);
            request.setData(null);
            request.setHeaders(sanitizeHeaders(request.getHeaders()));
        }
        List<Breadcrumb> breadcrumbs = event.getBreadcrumbs();
        if (breadcrumbs != null) {
            breadcrumbs.forEach(SentryEventSanitizer::sanitize);
        }
    }

    private static void sanitize(Breadcrumb breadcrumb) {
        URL_PART_DATA_KEYS.forEach(breadcrumb::removeData);
        if (breadcrumb.getData("url") instanceof String url) {
            breadcrumb.setData("url", withoutQuery(url));
        }
    }

    private static Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        if (headers == null) {
            return null;
        }
        Map<String, String> sanitized = new HashMap<>();
        headers.forEach((name, value) -> {
            String normalizedName = name.toLowerCase(Locale.ROOT);
            if (SENSITIVE_HEADERS.contains(normalizedName)) {
                return;
            }
            sanitized.put(name, REFERRER_HEADERS.contains(normalizedName) ? withoutQuery(value) : value);
        });
        return sanitized;
    }

    private static Map<String, Object> withoutUrlParts(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Map<String, Object> sanitized = new HashMap<>(data);
        sanitized.keySet().removeAll(URL_PART_DATA_KEYS);
        return sanitized;
    }

    private static String withoutQuery(String url) {
        return url == null ? null : QUERY_OR_FRAGMENT.matcher(url).replaceFirst("");
    }
}
