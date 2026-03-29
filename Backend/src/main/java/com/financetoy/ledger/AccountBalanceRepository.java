package com.financetoy.ledger;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, Long> {

    Optional<AccountBalance> findByExternalAccountId(String externalAccountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AccountBalance account where account.externalAccountId = :externalAccountId")
    Optional<AccountBalance> findByExternalAccountIdForUpdate(@Param("externalAccountId") String externalAccountId);
}
