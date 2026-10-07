package com.personal.happygallery.application.product;

import com.personal.happygallery.application.product.dto.SmartStoreNotice;
import com.personal.happygallery.application.product.dto.SmartStoreNoticePage;
import com.personal.happygallery.application.product.dto.SmartStoreNoticeSaveCommand;
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
    public SmartStoreNoticePage list(int page, int size) {
        requireEnabled();
        return provider.list(page, size);
    }

    @Override
    public SmartStoreNotice get(Long sellerNoticeId) {
        requireEnabled();
        return provider.get(sellerNoticeId);
    }

    @Override
    public Long create(SmartStoreNoticeSaveCommand command) {
        requireEnabled();
        return provider.create(command);
    }

    @Override
    public Long update(Long sellerNoticeId, SmartStoreNoticeSaveCommand command) {
        requireEnabled();
        return provider.update(sellerNoticeId, command);
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
}
