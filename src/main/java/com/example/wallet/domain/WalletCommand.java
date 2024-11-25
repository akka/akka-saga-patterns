package com.example.wallet.domain;

import java.math.BigDecimal;

public sealed interface WalletCommand {

  record CreateWallet(BigDecimal initialBalance) implements WalletCommand {
  }

  record ChargeWallet(BigDecimal amount, String reservationId) implements WalletCommand {
  }
}
