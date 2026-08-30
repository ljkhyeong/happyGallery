package com.personal.happygallery.application.pass.port.out;

import com.personal.happygallery.domain.pass.PassPurchase;

public interface PassPurchaseStorePort {

    <S extends PassPurchase> S save(S passPurchase);
}
