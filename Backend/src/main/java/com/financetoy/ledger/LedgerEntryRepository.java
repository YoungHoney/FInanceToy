package com.financetoy.ledger;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByOrderId(String orderId);

    long countByOrderIdAndEntryType(String orderId, LedgerEntryType entryType);
}
