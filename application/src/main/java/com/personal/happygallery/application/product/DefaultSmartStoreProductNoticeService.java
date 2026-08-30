package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.port.in.SmartStoreProductNoticeUseCase;
import com.personal.happygallery.application.product.port.out.SmartStoreProductNoticeProvider;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.NEVER)
public class DefaultSmartStoreProductNoticeService implements SmartStoreProductNoticeUseCase {

    private final SmartStoreProductNoticeProvider provider;

    public DefaultSmartStoreProductNoticeService(SmartStoreProductNoticeProvider provider) {
        this.provider = provider;
    }

    @Override
    public NoticePage list(int page, int size) {
        requireEnabled();
        var pageResult = provider.list(page, size);
        return new NoticePage(
                pageResult.notices().stream()
                        .map(notice -> new NoticeSummary(
                                notice.sellerNoticeId(), notice.postCategoryType(), notice.title(),
                                notice.importantNotice(), notice.importantNoticeStartDate(),
                                notice.importantNoticeEndDate(), notice.wholeNotice(),
                                notice.displayStartDate(), notice.displayEndDate()))
                        .toList(),
                pageResult.page(), pageResult.size(),
                pageResult.totalElements(), pageResult.totalPages());
    }

    @Override
    public Notice get(Long sellerNoticeId) {
        requireEnabled();
        var notice = provider.get(sellerNoticeId);
        return new Notice(
                notice.sellerNoticeId(), notice.postCategoryType(), notice.title(),
                notice.importantNotice(), notice.importantNoticeStartDate(),
                notice.importantNoticeEndDate(), notice.wholeNotice(),
                notice.displayStartDate(), notice.displayEndDate(), notice.popup(),
                notice.popupStartDate(), notice.popupEndDate(), notice.detailContents());
    }

    @Override
    public Long create(SaveCommand command) {
        requireEnabled();
        return provider.create(toProviderCommand(command));
    }

    @Override
    public Long update(
            Long sellerNoticeId, SaveCommand command) {
        requireEnabled();
        return provider.update(sellerNoticeId, toProviderCommand(command));
    }

    @Override
    public void delete(Long sellerNoticeId) {
        requireEnabled();
        provider.delete(sellerNoticeId);
    }

    @Override
    public void apply(Long sellerNoticeId, List<Long> channelProductNos) {
        requireEnabled();
        provider.apply(sellerNoticeId, channelProductNos);
    }

    private void requireEnabled() {
        if (!provider.isEnabled()) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "스마트스토어 연동이 비활성화되어 있습니다.");
        }
    }

    private static SmartStoreProductNoticeProvider.SaveCommand toProviderCommand(
            SaveCommand command) {
        return new SmartStoreProductNoticeProvider.SaveCommand(
                command.postCategoryType(), command.title(), command.importantNotice(),
                command.importantNoticeStartDate(), command.importantNoticeEndDate(),
                command.wholeNotice(), command.displayStartDate(), command.displayEndDate(),
                command.popup(), command.popupStartDate(), command.popupEndDate(),
                command.detailContents());
    }
}
