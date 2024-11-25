package com.example.cinema.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.keyvalueentity.KeyValueEntity;
import com.example.cinema.domain.Reservation;

import static akka.Done.done;

@ComponentId("reservation")
public class ReservationEntity extends KeyValueEntity<Reservation> {

  public Effect<Done> create(String showId) {
    String reservationId = commandContext().entityId();
    return effects().updateState(new Reservation(reservationId, showId)).thenReply(done());
  }

  public Effect<Reservation> get() {
    if (currentState() == null) {
      return effects().error("reservation not found");
    } else {
      return effects().reply(currentState());
    }
  }

  public Effect<Done> delete() {
    return effects().deleteEntity().thenReply(done());
  }
}
