package com.example.wallet.domain;


import akka.javasdk.annotations.TypeName;

import java.math.BigDecimal;

public sealed interface WalletEvent {

  @TypeName("wallet-created")
  record WalletCreated(String walletId, BigDecimal initialBalance) implements WalletEvent {
  }

  @TypeName("wallet-charged")
  record WalletCharged(String walletId, BigDecimal amount, String reservationId) implements WalletEvent {
  }

  @TypeName("wallet-charge-rejected")
  record WalletChargeRejected(String walletId, String reservationId) implements WalletEvent {
  }
}
