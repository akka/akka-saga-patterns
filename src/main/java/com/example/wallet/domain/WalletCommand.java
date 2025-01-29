package com.example.wallet.domain;

import java.math.BigDecimal;

public sealed interface WalletCommand {

  sealed interface RequiresDeduplicationCommand extends WalletCommand {
    String commandId();
  }

  record CreateWallet(String walletId, BigDecimal initialBalance) implements WalletCommand {
  }

  record ChargeWallet(BigDecimal amount, String expenseId,
                      String commandId) implements RequiresDeduplicationCommand {
  }

  record DepositFunds(BigDecimal amount, String commandId) implements RequiresDeduplicationCommand {
  }

  record Refund(String expenseId, String commandId) implements RequiresDeduplicationCommand {
  }
}
