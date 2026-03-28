package com.financetoy.ledger;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {

    Optional<AccountBalance> findByExternalAccountId(String externalAccountId);
}
