package com.personal.happygallery.app.pass.port.out;

import com.personal.happygallery.domain.pass.PassLedger;
import java.util.List;

public interface PassLedgerReaderPort {
    List<PassLedger> findByPassPurchaseId(Long passPurchaseId);
}
