package com.personal.happygallery.application.pass.port.out;

import com.personal.happygallery.domain.pass.PassLedger;
import java.util.List;

public interface PassLedgerReaderPort {
    List<PassLedger> findByPassPurchaseId(Long passPurchaseId);
}
