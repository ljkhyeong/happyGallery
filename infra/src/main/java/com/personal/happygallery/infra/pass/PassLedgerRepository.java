package com.personal.happygallery.infra.pass;

import com.personal.happygallery.domain.pass.PassLedger;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassLedgerRepository extends JpaRepository<PassLedger, Long> {

    List<PassLedger> findByPassPurchaseId(Long passPurchaseId);
}
