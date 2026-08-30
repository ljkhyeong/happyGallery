package com.personal.happygallery.application.pass.port.out;

import com.personal.happygallery.domain.pass.PassLedger;

public interface PassLedgerStorePort {

    <S extends PassLedger> S save(S ledger);
}
