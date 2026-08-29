package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.application.product.port.out.SmartStoreProductNoticeProvider;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NaverCommerceProductNoticeProvider implements SmartStoreProductNoticeProvider {

    private final RestClient restClient;
    private final SmartStoreProperties properties;
    private final NaverCommerceAccessTokenProvider accessTokenProvider;

    public NaverCommerceProductNoticeProvider(
            RestClient smartStoreRestClient,
            SmartStoreProperties properties,
            NaverCommerceAccessTokenProvider accessTokenProvider) {
        this.restClient = smartStoreRestClient;
        this.properties = properties;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled();
    }

    @Override
    public NoticePage list(int page, int size) {
        NoticeListResponse response = accessTokenProvider.authorized(token -> restClient.get()
                .uri(builder -> builder.path("/external/v1/contents/seller-notices")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(NoticeListResponse.class));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 상품 공지 목록 응답이 비어 있습니다.");
        }
        List<NoticeSummary> notices = response.contents() == null
                ? List.of()
                : response.contents().stream().map(RemoteNoticeSummary::toNotice).toList();
        return new NoticePage(
                notices, response.page(), response.size(),
                response.totalElements(), response.totalPages());
    }

    @Override
    public Notice get(Long sellerNoticeId) {
        RemoteNotice response = accessTokenProvider.authorized(token -> restClient.get()
                .uri("/external/v1/contents/seller-notices/{sellerNoticeId}", sellerNoticeId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(RemoteNotice.class));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 상품 공지 상세 응답이 비어 있습니다.");
        }
        return response.toNotice();
    }

    @Override
    public Long create(SaveCommand command) {
        NoticeIdResponse response = accessTokenProvider.authorized(token -> restClient.post()
                .uri("/external/v1/contents/seller-notices")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(NoticeRequest.from(command))
                .retrieve()
                .body(NoticeIdResponse.class));
        return noticeId(response);
    }

    @Override
    public Long update(Long sellerNoticeId, SaveCommand command) {
        NoticeIdResponse response = accessTokenProvider.authorized(token -> restClient.put()
                .uri("/external/v1/contents/seller-notices/{sellerNoticeId}", sellerNoticeId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(NoticeRequest.from(command))
                .retrieve()
                .body(NoticeIdResponse.class));
        return noticeId(response);
    }

    @Override
    public void delete(Long sellerNoticeId) {
        accessTokenProvider.authorized(token -> {
            restClient.delete()
                    .uri("/external/v1/contents/seller-notices/{sellerNoticeId}", sellerNoticeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    @Override
    public void apply(Long sellerNoticeId, List<Long> channelProductNos) {
        accessTokenProvider.authorized(token -> {
            restClient.put()
                    .uri("/external/v1/products/channel-products/notice/apply")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ApplyNoticeRequest(sellerNoticeId, channelProductNos))
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    private static Long noticeId(NoticeIdResponse response) {
        if (response == null) {
            throw new IllegalStateException("스마트스토어 상품 공지 저장 응답이 비어 있습니다.");
        }
        Long sellerNoticeId = response.sellerNoticeId() == null && response.data() != null
                ? response.data().sellerNoticeId() : response.sellerNoticeId();
        if (sellerNoticeId == null) {
            throw new IllegalStateException("스마트스토어 상품 공지 번호가 비어 있습니다.");
        }
        return sellerNoticeId;
    }

    private record NoticeListResponse(
            List<RemoteNoticeSummary> contents,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    private record RemoteNoticeSummary(
            Long sellerNoticeId,
            String postCategoryType,
            String title,
            boolean importantNotice,
            OffsetDateTime importantNoticeStartDate,
            OffsetDateTime importantNoticeEndDate,
            boolean wholeNotice,
            OffsetDateTime displayStartDate,
            OffsetDateTime displayEndDate
    ) {
        private NoticeSummary toNotice() {
            return new NoticeSummary(
                    sellerNoticeId, postCategoryType, title, importantNotice,
                    importantNoticeStartDate, importantNoticeEndDate, wholeNotice,
                    displayStartDate, displayEndDate);
        }
    }

    private record RemoteNotice(
            Long sellerNoticeId,
            String postCategoryType,
            String title,
            boolean importantNotice,
            OffsetDateTime importantNoticeStartDate,
            OffsetDateTime importantNoticeEndDate,
            boolean wholeNotice,
            OffsetDateTime displayStartDate,
            OffsetDateTime displayEndDate,
            boolean popup,
            OffsetDateTime popupStartDate,
            OffsetDateTime popupEndDate,
            String detailContents
    ) {
        private Notice toNotice() {
            return new Notice(
                    sellerNoticeId, postCategoryType, title, importantNotice,
                    importantNoticeStartDate, importantNoticeEndDate, wholeNotice,
                    displayStartDate, displayEndDate, popup, popupStartDate, popupEndDate,
                    detailContents);
        }
    }

    private record NoticeRequest(
            String postCategoryType,
            String title,
            boolean importantNotice,
            OffsetDateTime importantNoticeStartDate,
            OffsetDateTime importantNoticeEndDate,
            boolean wholeNotice,
            OffsetDateTime displayStartDate,
            OffsetDateTime displayEndDate,
            boolean popup,
            OffsetDateTime popupStartDate,
            OffsetDateTime popupEndDate,
            String detailContents
    ) {
        private static NoticeRequest from(SaveCommand command) {
            return new NoticeRequest(
                    command.postCategoryType(), command.title(), command.importantNotice(),
                    command.importantNoticeStartDate(), command.importantNoticeEndDate(),
                    command.wholeNotice(), command.displayStartDate(), command.displayEndDate(),
                    command.popup(), command.popupStartDate(), command.popupEndDate(),
                    command.detailContents());
        }
    }

    private record NoticeIdResponse(Long sellerNoticeId, NoticeIdData data) {}

    private record NoticeIdData(Long sellerNoticeId) {}

    private record ApplyNoticeRequest(Long sellerNoticeId, List<Long> channelProductNos) {}
}
