package com.example.cinema.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import com.example.wallet.domain.WalletCommand.ChargeWallet;

import static akka.Done.done;

@ComponentId("wallet-failure")
public class WalletFailureEntity extends EventSourcedEntity<WalletFailureEntity.WalletFailureState, WalletFailureEntity.WalletFailureEvent> {

  public record WalletFailureState(int numberOfFailures) {
    public WalletFailureState inc() {
      return new WalletFailureState(numberOfFailures + 1);
    }
  }

  public record RegisterChargeFailure(ChargeWallet source, String msg) {
  }

  sealed interface WalletFailureEvent {
  }

  public record WalletChargeFailureOccurred(ChargeWallet source, String msg) implements WalletFailureEvent {
  }

  @Override
  public WalletFailureState emptyState() {
    return new WalletFailureState(0);
  }

  @Override
  public WalletFailureState applyEvent(WalletFailureEvent walletFailureEvent) {
    return currentState().inc();
  }

  public Effect<Done> registerFailure(RegisterChargeFailure registerChargeFailure) {
    return effects()
      .persist(new WalletChargeFailureOccurred(registerChargeFailure.source, registerChargeFailure.msg))
      .thenReply(__ -> done());
  }
}
