package com.example.wallet.domain;

import com.example.wallet.domain.WalletCommand.ChargeWallet;
import com.example.wallet.domain.WalletCommand.CreateWallet;
import com.example.wallet.domain.WalletCommand.DepositFunds;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.example.cinema.domain.DomainGenerators.randomCommandId;
import static com.example.wallet.domain.WalletCommandError.DUPLICATED_COMMAND;
import static org.assertj.core.api.Assertions.assertThat;

class WalletTest {

  @Test
  public void shouldCreateWallet() {
    //given
    var wallet = Wallet.EMPTY;
    var createWallet = new CreateWallet("1", BigDecimal.TEN);

    //when
    var event = wallet.process(createWallet).rightValue();
    var updatedWallet = wallet.apply(event);

    //then
    assertThat(updatedWallet.id()).isEqualTo(createWallet.walletId());
    assertThat(updatedWallet.balance()).isEqualTo(createWallet.initialBalance());
  }

  @Test
  public void shouldRejectCommandIfWalletExists() {
    //given
    var wallet = new Wallet("1", BigDecimal.TEN);
    var createWallet = new CreateWallet("1", BigDecimal.TEN);

    //when
    var error = wallet.process(createWallet).leftValue();

    //then
    assertThat(error).isEqualTo(WalletCommandError.WALLET_ALREADY_EXISTS);
  }

  @Test
  public void shouldDepositFunds() {
    //given
    var wallet = new Wallet("1", BigDecimal.TEN);
    var depositFunds = new DepositFunds(BigDecimal.TEN, randomCommandId());

    //when
    var event = wallet.process(depositFunds).rightValue();
    var updatedWallet = wallet.apply(event);

    //then
    assertThat(updatedWallet.balance()).isEqualTo(BigDecimal.valueOf(20));
  }

  @Test
  public void shouldRejectDuplicatedDeposit() {
    //given
    var wallet = new Wallet("1", BigDecimal.TEN);
    var depositFunds = new DepositFunds(BigDecimal.TEN, randomCommandId());

    var event = wallet.process(depositFunds).rightValue();
    var updatedWallet = wallet.apply(event);

    //when
    var error = updatedWallet.process(depositFunds).leftValue();

    //then
    assertThat(error).isEqualTo(DUPLICATED_COMMAND);
  }

  @Test
  public void shouldChargeWallet() {
    //given
    var wallet = new Wallet("1", BigDecimal.TEN);
    var chargeWallet = new ChargeWallet(BigDecimal.valueOf(3), "abc", randomCommandId());

    //when
    var event = wallet.process(chargeWallet).rightValue();
    var updatedWallet = wallet.apply(event);

    //then
    assertThat(updatedWallet.balance()).isEqualTo(BigDecimal.valueOf(7));
  }

  @Test
  public void shouldRejectDuplicatedCharge() {
    //given
    var wallet = new Wallet("1", BigDecimal.TEN);
    var chargeWallet = new ChargeWallet(BigDecimal.valueOf(3), "abc", randomCommandId());

    var event = wallet.process(chargeWallet).rightValue();
    var updatedWallet = wallet.apply(event);

    //when
    var error = updatedWallet.process(chargeWallet).leftValue();

    //then
    assertThat(error).isEqualTo(DUPLICATED_COMMAND);
  }
}