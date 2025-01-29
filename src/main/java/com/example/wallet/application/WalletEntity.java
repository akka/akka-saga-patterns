package com.example.wallet.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import com.example.common.Or;
import com.example.common.Response;
import com.example.common.Response.Failure;
import com.example.common.Response.Success;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletCommand;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import com.example.wallet.domain.WalletCommand.CreateWallet;
import com.example.wallet.domain.WalletCommand.DepositFunds;
import com.example.wallet.domain.WalletCommand.Refund;
import com.example.wallet.domain.WalletCommandError;
import com.example.wallet.domain.WalletEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static com.example.wallet.domain.WalletCommandError.EXPENSE_NOT_FOUND;

@ComponentId("wallet")
public class WalletEntity extends EventSourcedEntity<Wallet, WalletEvent> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public Wallet emptyState() {
    return Wallet.EMPTY;
  }

  public Effect<Response> create(CreateWallet createWallet) {
    return switch (currentState().process(createWallet)) {
      case Or.Left(var error) -> errorEffect(error, createWallet);
      case Or.Right(var event) -> persistEffect(event, "wallet created", createWallet);
    };
  }

  public Effect<WalletResponse> get() {
    if (currentState().isEmpty()) {
      return effects().error("wallet not created");
    } else {
      return effects().reply(WalletResponse.from(currentState()));
    }
  }

  public Effect<Response> charge(ChargeWallet chargeWallet) {
    // simulate a failure for expenseId=42 but also allow skipping the failure simulation
    if (chargeWallet.expenseId().equals("42") && commandContext().metadata().get("skip-failure-simulation").isEmpty()) {
      logger.info("charging failed");
      return effects().error("Unexpected error for expenseId=42");
    } else {
      return switch (currentState().process(chargeWallet)) {
        case Or.Left(var error) -> errorEffect(error, chargeWallet);
        case Or.Right(var event) -> persistEffect(event, e -> {
          if (e instanceof WalletEvent.WalletChargeRejected) {
            return Failure.of("wallet charge rejected");
          } else {
            return Success.of("wallet charged");
          }
        }, chargeWallet);
      };
    }
  }

  public Effect<Response> deposit(DepositFunds depositFunds) {
    return switch (currentState().process(depositFunds)) {
      case Or.Left(var error) -> errorEffect(error, depositFunds);
      case Or.Right(var event) -> persistEffect(event, "funds deposited", depositFunds);
    };
  }

  public Effect<Response> refund(Refund refund) {
    return switch (currentState().process(refund)) {
      case Or.Left(var error) -> {
        if (error == EXPENSE_NOT_FOUND) {
          //ignoring
          yield effects().reply(Success.of("ok"));
        } else {
          yield errorEffect(error, refund);
        }
      }
      case Or.Right(var event) -> persistEffect(event, "funds refunded", refund);
    };
  }

  @Override
  public Wallet applyEvent(WalletEvent walletEvent) {
    return currentState().apply(walletEvent);
  }

  private Effect<Response> persistEffect(WalletEvent event, String replyMessage, WalletCommand walletCommand) {
    return persistEffect(event, e -> Success.of(replyMessage), walletCommand);
  }

  private Effect<Response> persistEffect(WalletEvent event, Function<WalletEvent, Response> eventToResponse, WalletCommand walletCommand) {
    return effects()
      .persist(event)
      .thenReply(__ -> {
        logger.info("processing command {} completed", walletCommand);
        return eventToResponse.apply(event);
      });
  }

  private Effect<Response> errorEffect(WalletCommandError error, WalletCommand walletCommand) {
    if (error.equals(WalletCommandError.DUPLICATED_COMMAND)) {
      logger.debug("Ignoring duplicated command {}", walletCommand);
      return effects().reply(Success.of("ok"));
    } else {
      logger.warn("processing {} failed with {}", walletCommand, error);
      return effects().reply(Failure.of(error.name()));
    }
  }
}
