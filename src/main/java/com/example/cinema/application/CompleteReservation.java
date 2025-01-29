package com.example.cinema.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import com.example.cinema.domain.Reservation;
import com.example.cinema.domain.ShowByReservation;
import com.example.cinema.domain.ShowCommand;
import com.example.cinema.domain.ShowCommand.CancelSeatReservation;
import com.example.common.Response;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.domain.WalletEvent.WalletChargeRejected;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

@ComponentId("complete-reservation")
@Consume.FromEventSourcedEntity(value = WalletEntity.class, ignoreUnknown = true)
public class CompleteReservation extends Consumer {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ComponentClient componentClient;

  public CompleteReservation(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect confirmReservation(WalletCharged walletCharged) {
    logger.info("confirming reservation, triggered by {}", walletCharged);

    String reservationId = walletCharged.expenseId();

    return effects().asyncDone(
      getShowIdBy(reservationId)
        .thenCompose(showId -> confirmReservation(showId, reservationId))
    );
  }

  public Effect cancelReservation(WalletChargeRejected walletChargeRejected) {
    logger.info("cancelling reservation, triggered by {}", walletChargeRejected);

    String reservationId = walletChargeRejected.expenseId();

    return effects().asyncDone(
      getShowIdBy(reservationId)
        .thenCompose(showId -> cancelReservation(showId, reservationId))
    );
  }

  private CompletionStage<Done> confirmReservation(String showId, String reservationId) {
    return componentClient.forEventSourcedEntity(showId)
      .method(ShowEntity::confirmPayment)
      .invokeAsync(new ShowCommand.ConfirmReservationPayment(reservationId))
      .thenApply(Response::toDone);
  }

  private CompletionStage<Done> cancelReservation(String showId, String reservationId) {
    return componentClient.forEventSourcedEntity(showId)
      .method(ShowEntity::cancelReservation)
      .invokeAsync(new CancelSeatReservation(reservationId))
      .thenApply(Response::toDone);
  }

  //Key Value Entity as a read model
  private CompletionStage<String> getShowIdBy(String reservationId) {
    return componentClient.forKeyValueEntity(reservationId)
      .method(ReservationEntity::get)
      .invokeAsync()
      .thenApply(Reservation::showId);
  }

  //View as a read model
  private CompletionStage<String> getShowIdBy2(String reservationId) {
    return componentClient.forView()
      .method(ShowByReservationView::getShow)
      .invokeAsync(reservationId)
      .thenApply(ShowByReservation::showId);
  }
}
