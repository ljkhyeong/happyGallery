package com.personal.happygallery.app.pass.port.out;

import com.personal.happygallery.domain.pass.PassLedger;

public interface PassLedgerStorePort {
    PassLedger save(PassLedger ledger);
}
