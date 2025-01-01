package com.example.wallet.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import com.example.common.Or;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletCommand;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import com.example.wallet.domain.WalletCommand.CreateWallet;
import com.example.wallet.domain.WalletCommand.DepositFunds;
import com.example.wallet.domain.WalletCommandError;
import com.example.wallet.domain.WalletEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static akka.Done.done;

@ComponentId("wallet")
public class WalletEntity extends EventSourcedEntity<Wallet, WalletEvent> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public Wallet emptyState() {
    return Wallet.EMPTY;
  }

  public Effect<Done> create(CreateWallet createWallet) {
    return switch (currentState().process(createWallet)) {
      case Or.Left(var error) -> errorEffect(error, createWallet);
      case Or.Right(var event) -> persistEffect(event, createWallet);
    };
  }

  public Effect<WalletResponse> get() {
    if (currentState().isEmpty()) {
      return effects().error("wallet not created");
    } else {
      return effects().reply(WalletResponse.from(currentState()));
    }
  }

  public Effect<Done> charge(ChargeWallet chargeWallet) {
    // simulate a failure for expenseId=42 but also allow skipping the failure simulation
    if (chargeWallet.expenseId().equals("42") && commandContext().metadata().get("skip-failure-simulation").isEmpty()) {
      logger.info("charging failed");
      return effects().error("Unexpected error for expenseId=42");
    } else {
      return switch (currentState().process(chargeWallet)) {
        case Or.Left(var error) -> errorEffect(error, chargeWallet);
        case Or.Right(var event) -> persistEffect(event, chargeWallet);
      };
    }
  }

  public Effect<Done> deposit(DepositFunds depositFunds) {
    return switch (currentState().process(depositFunds)) {
      case Or.Left(var error) -> errorEffect(error, depositFunds);
      case Or.Right(var event) -> persistEffect(event, depositFunds);
    };
  }

  @Override
  public Wallet applyEvent(WalletEvent walletEvent) {
    return currentState().apply(walletEvent);
  }

  private Effect<Done> persistEffect(WalletEvent event, WalletCommand walletCommand) {
    return effects()
      .persist(event)
      .thenReply(__ -> {
        logger.info("processing command {} completed", walletCommand);
        return done();
      });
  }

  private Effect<Done> errorEffect(WalletCommandError error, WalletCommand walletCommand) {
    if (error.equals(WalletCommandError.DUPLICATED_COMMAND)) {
      logger.debug("Ignoring duplicated command {}", walletCommand);
      return effects().reply(done());
    } else {
      logger.warn("processing {} failed with {}", walletCommand, error);
      return effects().error(error.name());
    }
  }
}
