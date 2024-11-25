package com.example.cinema.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import com.example.cinema.domain.SeatStatus;
import com.example.cinema.domain.Show;
import com.example.cinema.domain.ShowCommand;
import com.example.cinema.domain.ShowCommand.CancelSeatReservation;
import com.example.cinema.domain.ShowCommand.ConfirmReservationPayment;
import com.example.cinema.domain.ShowCommand.ReserveSeat;
import com.example.cinema.domain.ShowCreator;
import com.example.cinema.domain.ShowEvent;
import com.example.cinema.domain.ShowEvent.ShowCreated;
import com.example.common.Or;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static akka.Done.done;

@ComponentId("show")
public class ShowEntity extends EventSourcedEntity<Show, ShowEvent> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public Effect<Done> create(ShowCommand.CreateShow createShow) {
    if (currentState() != null) {
      return effects().error("show already exists");
    } else {
      return switch (ShowCreator.create(commandContext().entityId(), createShow)) {
        case Or.Left(var error) -> effects().error(error.name());
        case Or.Right(var showCreated) -> effects()
          .persist(showCreated)
          .thenReply(__ -> done());
      };
    }
  }

  public Effect<Done> reserve(ReserveSeat reserveSeat) {
    if (currentState() == null) {
      return effects().error("show not exists");
    } else {
      return switch (currentState().process(reserveSeat)) {
        case Or.Left(var error) -> effects().error(error.name());
        case Or.Right(var showEvent) -> effects()
          .persist(showEvent)
          .thenReply(__ -> done());
      };
    }
  }

  public Effect<Done> cancelReservation(CancelSeatReservation cancelSeatReservation) {
    if (currentState() == null) {
      return effects().error("show not exists");
    } else {
      return switch (currentState().process(cancelSeatReservation)) {
        case Or.Left(var error) -> effects().error(error.name());
        case Or.Right(var showEvent) -> effects()
          .persist(showEvent)
          .thenReply(__ -> done());
      };
    }
  }

  public Effect<Done> confirmPayment(ConfirmReservationPayment confirmReservationPayment) {
    if (currentState() == null) {
      return effects().error("show not exists");
    } else {
      return switch (currentState().process(confirmReservationPayment)) {
        case Or.Left(var error) -> effects().error(error.name());
        case Or.Right(var showEvent) -> effects()
          .persist(showEvent)
          .thenReply(__ -> done());
      };
    }
  }

  public Effect<ShowResponse> get() {
    if (currentState() == null) {
      return effects().error("show not exists");
    } else {
      return effects().reply(ShowResponse.from(currentState()));
    }
  }

  public Effect<SeatStatus> getSeatStatus(int seatNumber) {
    if (currentState() == null) {
      return effects().error("show not exists");
    } else {
      return currentState().seats().get(seatNumber).fold(
        () -> effects().error("seat not exists"),
        seat -> effects().reply(seat.status())
      );
    }
  }

  @Override
  public Show applyEvent(ShowEvent showEvent) {
    if (showEvent instanceof ShowCreated created) {
      return Show.create(created);
    }
    return currentState().apply(showEvent);
  }
}
