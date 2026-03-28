package com.financetoy.common.config;

import com.financetoy.ledger.AccountBalance;
import com.financetoy.ledger.AccountBalanceRepository;
import java.math.BigDecimal;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final AccountBalanceRepository accountBalanceRepository;

    public DataInitializer(AccountBalanceRepository accountBalanceRepository) {
        this.accountBalanceRepository = accountBalanceRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        accountBalanceRepository.findByExternalAccountId(DemoAccounts.DEFAULT_ACCOUNT_ID)
                .orElseGet(() -> accountBalanceRepository.save(
                        new AccountBalance(
                                DemoAccounts.DEFAULT_ACCOUNT_ID,
                                new BigDecimal("1000000.00"),
                                BigDecimal.ZERO
                        )));
    }
}
