package com.example.cinema.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.keyvalueentity.KeyValueEntity;
import com.example.cinema.domain.Reservation;

import java.math.BigDecimal;

import static akka.Done.done;

@ComponentId("reservation")
public class ReservationEntity extends KeyValueEntity<Reservation> {

  public record CreateReservation(String showId, String walletId, BigDecimal price) {
  }

  public Effect<Done> create(CreateReservation createReservation) {
    String reservationId = commandContext().entityId();
    return effects().updateState(new Reservation(reservationId, createReservation.showId, createReservation.walletId, createReservation.price)).thenReply(done());
  }

  public Effect<Reservation> get() {
    if (currentState() == null) {
      return effects().error("reservation not found: " + commandContext().entityId());
    } else {
      return effects().reply(currentState());
    }
  }

  public Effect<Done> delete() {
    return effects().deleteEntity().thenReply(done());
  }
}
