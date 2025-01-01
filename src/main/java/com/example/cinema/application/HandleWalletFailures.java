package com.example.cinema.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import com.example.cinema.application.WalletFailureEntity.WalletChargeFailureOccurred;
import com.example.cinema.domain.Reservation;
import com.example.cinema.domain.ShowCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

@ComponentId("handle-wallet-failures")
@Consume.FromEventSourcedEntity(WalletFailureEntity.class)
public class HandleWalletFailures extends Consumer {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ComponentClient componentClient;

  public HandleWalletFailures(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect handle(WalletChargeFailureOccurred walletChargeFailureOccurred) {
    logger.info("handling failure: {}", walletChargeFailureOccurred);

    String reservationId = walletChargeFailureOccurred.source().expenseId();

    return effects().asyncDone(getShowIdBy(reservationId).thenCompose(showId ->
      cancelReservation(reservationId, showId)
    ));
  }

  private CompletionStage<Done> cancelReservation(String reservationId, String showId) {
    return componentClient.forEventSourcedEntity(showId)
      .method(ShowEntity::cancelReservation)
      .invokeAsync(new ShowCommand.CancelSeatReservation(reservationId));
  }

  private CompletionStage<String> getShowIdBy(String reservationId) {
    return componentClient.forKeyValueEntity(reservationId).method(ReservationEntity::get)
      .invokeAsync()
      .thenApply(Reservation::showId);
  }
}
