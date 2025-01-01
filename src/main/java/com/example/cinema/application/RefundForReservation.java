package com.example.cinema.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import com.example.cinema.domain.Reservation;
import com.example.cinema.domain.ShowEvent.CancelledReservationConfirmed;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.domain.WalletCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static java.nio.charset.StandardCharsets.UTF_8;

@ComponentId("refund-for-reservation")
@Consume.FromEventSourcedEntity(value = ShowEntity.class, ignoreUnknown = true)
public class RefundForReservation extends Consumer {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ComponentClient componentClient;

  public RefundForReservation(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect refund(CancelledReservationConfirmed cancelledReservationConfirmed) {
    logger.info("refunding for reservation, triggered by " + cancelledReservationConfirmed);

    String sequenceNum = messageContext().metadata().get("ce-sequence").orElseThrow();
    String commandId = UUID.nameUUIDFromBytes(sequenceNum.getBytes(UTF_8)).toString();

    return effects().asyncDone(
      getReservation(cancelledReservationConfirmed.reservationId()).thenCompose(reservation ->
        refund(reservation.walletId(), reservation.price(), commandId)
      )
    );
  }

  private CompletionStage<Reservation> getReservation(String reservationId) {
    return componentClient.forKeyValueEntity(reservationId)
      .method(ReservationEntity::get)
      .invokeAsync();
  }

  private CompletionStage<Done> refund(String walletId, BigDecimal amount, String commandId) {
    return componentClient.forEventSourcedEntity(walletId)
      .method(WalletEntity::deposit)
      .invokeAsync(new WalletCommand.DepositFunds(amount, commandId));
  }
}
