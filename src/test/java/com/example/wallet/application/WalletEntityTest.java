package com.example.wallet.application;

import akka.Done;
import akka.javasdk.testkit.EventSourcedResult;
import akka.javasdk.testkit.EventSourcedTestKit;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletCommand;
import com.example.wallet.domain.WalletCommand.CreateWallet;
import com.example.wallet.domain.WalletEvent;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import com.example.wallet.domain.WalletEvent.WalletCreated;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.example.cinema.domain.DomainGenerators.randomWalletId;
import static org.assertj.core.api.Assertions.assertThat;

class WalletEntityTest {


  @Test
  public void shouldCreateWallet() {
    //given
    var walletId = randomWalletId();
    var initialBalance = 100;
    EventSourcedTestKit<Wallet, WalletEvent, WalletEntity> testKit = EventSourcedTestKit.of(walletId, WalletEntity::new);
    CreateWallet createWallet = new CreateWallet(BigDecimal.valueOf(100));

    //when
    EventSourcedResult<Done> result = testKit.call(wallet -> wallet.create(createWallet));

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
    CreateWallet createWallet = new CreateWallet(BigDecimal.valueOf(100));
    testKit.call(wallet -> wallet.create(createWallet));
    var chargeWallet = new WalletCommand.ChargeWallet(new BigDecimal(10), "r1");

    //when
    EventSourcedResult<Done> result = testKit.call(wallet -> wallet.charge(chargeWallet));

    //then
    WalletCharged charged = result.getNextEventOfType(WalletCharged.class);
    assertThat(result.isReply()).isTrue();
    assertThat(charged.amount()).isEqualTo(chargeWallet.amount());
    assertThat(charged.reservationId()).isEqualTo(chargeWallet.reservationId());
//    assertThat(testKit.getState().id()).isEqualTo(walletId);
    assertThat(testKit.getState().balance()).isEqualTo(new BigDecimal(90));
  }
}