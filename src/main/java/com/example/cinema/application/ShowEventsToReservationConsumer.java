package com.example.cinema.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import com.example.cinema.domain.ShowEvent.SeatReservationCancelled;
import com.example.cinema.domain.ShowEvent.SeatReservationPaid;
import com.example.cinema.domain.ShowEvent.SeatReserved;
import com.example.wallet.application.WalletEntity;

import java.util.concurrent.CompletionStage;

@ComponentId("show-events-to-reservation-consumer")
@Consume.FromEventSourcedEntity(value = ShowEntity.class, ignoreUnknown = true)
public class ShowEventsToReservationConsumer extends Consumer {

  private final ComponentClient componentClient;

  public ShowEventsToReservationConsumer(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect onEvent(SeatReserved reserved) {
    return effects().asyncDone(createReservation(reserved.reservationId(), reserved.showId()));
  }

  public Effect onEvent(SeatReservationPaid paid) {
    return effects().asyncDone(deleteReservation(paid.reservationId()));
  }

  public Effect onEvent(SeatReservationCancelled cancelled) {
    return effects().asyncDone(deleteReservation(cancelled.reservationId()));
  }

  private CompletionStage<Done> createReservation(String reservationId, String showId) {
    return componentClient.forKeyValueEntity(reservationId).method(ReservationEntity::create).invokeAsync(showId);
  }

  private CompletionStage<Done> deleteReservation(String reservationId) {
    return componentClient.forKeyValueEntity(reservationId).method(ReservationEntity::delete).invokeAsync();
  }
}
