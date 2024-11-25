package com.example.wallet.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import com.example.common.Or;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import com.example.wallet.domain.WalletCommand.CreateWallet;
import com.example.wallet.domain.WalletEvent;
import com.example.wallet.domain.WalletEvent.WalletCreated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static akka.Done.done;

@ComponentId("wallet")
public class WalletEntity extends EventSourcedEntity<Wallet, WalletEvent> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public Wallet emptyState() {
    return Wallet.EMPTY;
  }

  public Effect<Done> create(CreateWallet createWallet) {
    if (!currentState().isEmpty()) {
      logger.warn("wallet already exists");
      return effects().error("wallet already exists");
    } else {
      String walletId = commandContext().entityId();
      BigDecimal initialBalance = createWallet.initialBalance();
      return effects()
        .persist(new WalletCreated(walletId, initialBalance))
        .thenReply(__ -> {
          logger.info("wallet {} created, init balance {}", walletId, initialBalance);
          return done();
        });
    }
  }

  public Effect<Wallet> get() {
    if (currentState().isEmpty()) {
      return effects().error("wallet not created");
    } else {
      return effects().reply(currentState());
    }
  }

  public Effect<Done> charge(ChargeWallet chargeWallet) {
    if (currentState().isEmpty()) {
      logger.error("wallet not exists");
      return effects().error("wallet not exists");
    } else {
      return switch (currentState().process(chargeWallet)) {
        case Or.Left(var error) -> {
          logger.error("processing command {} failed with {}", chargeWallet, error);
          yield effects().error(error.name());
        }
        case Or.Right(var event) -> {
          yield effects().persist(event).thenReply(__ -> {
            logger.info("charging wallet completed {}", chargeWallet);
            return done();
          });
        }
      };
    }
  }

  @Override
  public Wallet applyEvent(WalletEvent walletEvent) {
    return currentState().apply(walletEvent);
  }
}
