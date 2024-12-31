package com.example.wallet.application;

import com.example.wallet.domain.Wallet;

import java.math.BigDecimal;

public record WalletResponse(String id, BigDecimal balance) {
  public static WalletResponse from(Wallet wallet) {
    return new WalletResponse(wallet.id(), wallet.balance());
  }
}
