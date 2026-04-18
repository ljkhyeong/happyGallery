package com.personal.happygallery.application.pass.port.out;

import com.personal.happygallery.domain.pass.PassPurchase;

public interface PassPurchaseStorePort {
    PassPurchase save(PassPurchase passPurchase);
}
