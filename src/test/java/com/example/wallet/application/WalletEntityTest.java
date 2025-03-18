package com.example.wallet.application;

import akka.javasdk.testkit.EventSourcedResult;
import akka.javasdk.testkit.EventSourcedTestKit;
import com.example.common.Response;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletCommand;
import com.example.wallet.domain.WalletCommand.CreateWallet;
import com.example.wallet.domain.WalletEvent;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import com.example.wallet.domain.WalletEvent.WalletCreated;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.example.cinema.domain.DomainGenerators.randomCommandId;
import static com.example.cinema.domain.DomainGenerators.randomWalletId;
import static org.assertj.core.api.Assertions.assertThat;

class WalletEntityTest {


  @Test
  public void shouldCreateWallet() {
    //given
    var walletId = randomWalletId();
    var initialBalance = 100;
    EventSourcedTestKit<Wallet, WalletEvent, WalletEntity> testKit = EventSourcedTestKit.of(walletId, WalletEntity::new);
    CreateWallet createWallet = new CreateWallet(walletId, BigDecimal.valueOf(100));

    //when
    EventSourcedResult<Response> result = testKit.method(WalletEntity::create).invoke(createWallet);

    //then
    WalletCreated created = result.getNextEventOfType(WalletCreated.class);
    assertThat(result.isReply()).isTrue();
    assertThat(created.initialBalance()).isEqualTo(createWallet.initialBalance());
//    assertThat(testKit.getState().id()).isEqualTo(walletId);
    assertThat(testKit.getState().balance()).isEqualTo(BigDecimal.valueOf(initialBalance));
  }

  @Test
  public void shouldChargeWallet() {
    //given
    var walletId = randomWalletId();
    EventSourcedTestKit<Wallet, WalletEvent, WalletEntity> testKit = EventSourcedTestKit.of(walletId, WalletEntity::new);
    CreateWallet createWallet = new CreateWallet(walletId, BigDecimal.valueOf(100));
    testKit.method(WalletEntity::create).invoke(createWallet);
    var chargeWallet = new WalletCommand.ChargeWallet(new BigDecimal(10), "r1", randomCommandId());

    //when
    EventSourcedResult<Response> result = testKit.method(WalletEntity::charge).invoke(chargeWallet);

    //then
    WalletCharged charged = result.getNextEventOfType(WalletCharged.class);
    assertThat(result.isReply()).isTrue();
    assertThat(charged.amount()).isEqualTo(chargeWallet.amount());
    assertThat(charged.expenseId()).isEqualTo(chargeWallet.expenseId());
//    assertThat(testKit.getState().id()).isEqualTo(walletId);
    assertThat(testKit.getState().balance()).isEqualTo(new BigDecimal(90));
  }

  @Test
  public void shouldIgnoreChargeDuplicate() {
    //given
    var walletId = randomWalletId();
    EventSourcedTestKit<Wallet, WalletEvent, WalletEntity> testKit = EventSourcedTestKit.of(walletId, WalletEntity::new);
    CreateWallet createWallet = new CreateWallet(walletId, BigDecimal.valueOf(100));
    testKit.method(WalletEntity::create).invoke(createWallet);
    var chargeWallet = new WalletCommand.ChargeWallet(new BigDecimal(10), "r1", randomCommandId());
    testKit.method(WalletEntity::charge).invoke(chargeWallet);

    //when
    EventSourcedResult<Response> result = testKit.method(WalletEntity::charge).invoke(chargeWallet);

    //then
    assertThat(result.isReply()).isTrue();
    assertThat(result.didPersistEvents()).isFalse();
    assertThat(testKit.getState().balance()).isEqualTo(new BigDecimal(90));
  }

  @Test
  public void shouldRefundWallet() {
    //given
    var walletId = randomWalletId();
    EventSourcedTestKit<Wallet, WalletEvent, WalletEntity> testKit = EventSourcedTestKit.of(walletId, WalletEntity::new);
    CreateWallet createWallet = new CreateWallet(walletId, BigDecimal.valueOf(100));
    testKit.method(WalletEntity::create).invoke(createWallet);
    var chargeWallet = new WalletCommand.ChargeWallet(new BigDecimal(10), "r1", randomCommandId());
    testKit.method(WalletEntity::charge).invoke(chargeWallet);
    var refund = new WalletCommand.Refund("r1", randomCommandId());

    //when
    EventSourcedResult<Response> refundResult = testKit.method(WalletEntity::refund).invoke(refund);

    //then
    assertThat(refundResult.isReply()).isTrue();
    assertThat(refundResult.didPersistEvents()).isTrue();
    assertThat(testKit.getState().balance()).isEqualTo(new BigDecimal(100));
  }
}