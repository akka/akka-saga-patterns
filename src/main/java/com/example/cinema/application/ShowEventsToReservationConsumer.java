package com.example.cinema.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import com.example.cinema.application.ReservationEntity.CreateReservation;
import com.example.cinema.domain.ShowEvent.SeatReservationPaid;
import com.example.cinema.domain.ShowEvent.SeatReserved;

import java.util.concurrent.CompletionStage;

@ComponentId("show-events-to-reservation-consumer")
@Consume.FromEventSourcedEntity(value = ShowEntity.class, ignoreUnknown = true)
public class ShowEventsToReservationConsumer extends Consumer {

  private final ComponentClient componentClient;

  public ShowEventsToReservationConsumer(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect onEvent(SeatReserved reserved) {
    return effects().asyncDone(createReservation(reserved.reservationId(), new CreateReservation(reserved.showId(), reserved.walletId(), reserved.price())));
  }

  public Effect onEvent(SeatReservationPaid paid) {
    return effects().asyncDone(deleteReservation(paid.reservationId()));
  }

// alternatively we can use dedicated event for the cancellation after a failure
//  public Effect onEvent(SeatReservationCancelled cancelled) {
//    return effects().asyncDone(deleteReservation(cancelled.reservationId()));
//  }

  private CompletionStage<Done> createReservation(String reservationId, CreateReservation createReservation) {
    return componentClient.forKeyValueEntity(reservationId).method(ReservationEntity::create).invokeAsync(createReservation);
  }

  private CompletionStage<Done> deleteReservation(String reservationId) {
    return componentClient.forKeyValueEntity(reservationId).method(ReservationEntity::delete).invokeAsync();
  }
}
