package com.personal.happygallery.application.product.port.in;

import com.personal.happygallery.application.product.dto.SmartStoreNotice;
import com.personal.happygallery.application.product.dto.SmartStoreNoticePage;
import com.personal.happygallery.application.product.dto.SmartStoreNoticeSaveCommand;
import java.util.List;

public interface SmartStoreProductNoticeUseCase {

    SmartStoreNoticePage list(int page, int size);

    SmartStoreNotice get(Long sellerNoticeId);

    Long create(SmartStoreNoticeSaveCommand command);

    Long update(Long sellerNoticeId, SmartStoreNoticeSaveCommand command);

    void delete(Long sellerNoticeId);

    void apply(Long sellerNoticeId, List<Long> channelProductNos);
}
