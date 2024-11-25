package com.example.wallet.domain;

import com.example.common.Or;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import com.example.wallet.domain.WalletCommand.CreateWallet;
import com.example.wallet.domain.WalletEvent.WalletChargeRejected;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import com.example.wallet.domain.WalletEvent.WalletCreated;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

import static com.example.wallet.domain.WalletCommandError.WALLET_ALREADY_EXISTS;

public record Wallet(String id, BigDecimal balance) {

  public static final Wallet EMPTY = new Wallet("", BigDecimal.ZERO);

  @JsonIgnore
  public boolean isEmpty() {
    return id.equals(EMPTY.id);
  }

  public Or<WalletCommandError, WalletEvent> process(WalletCommand command) {
    return switch (command) {
      case CreateWallet ignored -> Or.left(WALLET_ALREADY_EXISTS);
      case ChargeWallet charge -> handleCharge(charge);
    };
  }

  private Or<WalletCommandError, WalletEvent> handleCharge(ChargeWallet charge) {
    if (balance.compareTo(charge.amount()) < 0) {
      return Or.right(new WalletChargeRejected(id, charge.reservationId()));
    } else {
      return Or.right(new WalletCharged(id, charge.amount(), charge.reservationId()));
    }
  }

  public Wallet apply(WalletEvent event) {
    return switch (event) {
      case WalletCreated created -> new Wallet(created.walletId(), created.initialBalance());
      case WalletCharged charged -> new Wallet(id, balance.subtract(charged.amount()));
      case WalletChargeRejected __ -> this;
    };
  }
}
