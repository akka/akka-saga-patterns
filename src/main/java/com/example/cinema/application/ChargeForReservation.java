package com.example.cinema.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import akka.pattern.Patterns;
import akka.stream.Materializer;
import com.example.cinema.domain.ShowEvent.SeatReserved;
import com.example.common.Response;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static java.nio.charset.StandardCharsets.UTF_8;

@ComponentId("charge-for-reservation")
@Consume.FromEventSourcedEntity(value = ShowEntity.class, ignoreUnknown = true)
public class ChargeForReservation extends Consumer {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ComponentClient componentClient;
  private final Materializer materializer;

  public ChargeForReservation(ComponentClient componentClient, Materializer materializer) {
    this.componentClient = componentClient;
    this.materializer = materializer;
  }

  public Effect charge(SeatReserved seatReserved) {
    logger.info("charging for reservation, triggered by {}", seatReserved);
    String expenseId = seatReserved.reservationId();
    String sequenceNum = messageContext().metadata().get("ce-sequence").orElseThrow();
    String commandId = UUID.nameUUIDFromBytes(sequenceNum.getBytes(UTF_8)).toString();
    var chargeWallet = new ChargeWallet(seatReserved.price(), expenseId, commandId);
    var walletId = seatReserved.walletId();


    var attempts = 3;
    var retryDelay = Duration.ofSeconds(1);

    return effects().asyncDone(
      Patterns.retry(() -> chargeWallet(walletId, chargeWallet),
          attempts,
          retryDelay,
          materializer.system())
        .exceptionallyComposeAsync(throwable ->
          registerFailure(throwable, walletId, chargeWallet)
        )
    );

  }

  private CompletionStage<Done> chargeWallet(String walletId, ChargeWallet chargeWallet) {
    return componentClient.forEventSourcedEntity(walletId)
      .method(WalletEntity::charge)
      .invokeAsync(chargeWallet)
      .thenApply(Response::toDone);
  }

  private CompletionStage<Done> registerFailure(Throwable throwable, String walletId, ChargeWallet chargeWallet) {
    var msg = throwable.getMessage(); //TODO

    return componentClient.forEventSourcedEntity(walletId)
      .method(WalletFailureEntity::registerFailure)
      .invokeAsync(new WalletFailureEntity.RegisterChargeFailure(chargeWallet, msg));
  }
}
