package com.personal.happygallery.application.pass.port.out;

import com.personal.happygallery.domain.pass.PassLedger;

public interface PassLedgerStorePort {
    PassLedger save(PassLedger ledger);
}
