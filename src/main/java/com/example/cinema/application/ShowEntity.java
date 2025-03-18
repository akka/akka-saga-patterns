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
import com.example.cinema.domain.ShowCommandError;
import com.example.cinema.domain.ShowCreator;
import com.example.cinema.domain.ShowEvent;
import com.example.cinema.domain.ShowEvent.ShowCreated;
import com.example.common.Or;
import com.example.common.Response;
import com.example.common.Response.Failure;
import com.example.common.Response.Success;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.function.Predicate;

import static com.example.cinema.domain.ShowCommandError.CANCELLING_CONFIRMED_RESERVATION;
import static com.example.cinema.domain.ShowCommandError.DUPLICATED_COMMAND;
import static com.example.cinema.domain.ShowCommandError.RESERVATION_NOT_FOUND;

@ComponentId("show")
public class ShowEntity extends EventSourcedEntity<Show, ShowEvent> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public Effect<Response> create(ShowCommand.CreateShow createShow) {
    if (currentState() != null) {
      return effects().error("show already exists");
    } else {
      return switch (ShowCreator.create(commandContext().entityId(), createShow)) {
        case Or.Left(var error) -> errorEffect(error, createShow);
        case Or.Right(var event) -> persistEffect(event);
      };
    }
  }

  public Effect<Response> reserve(ReserveSeat reserveSeat) {
    if (currentState() == null) {
      return effects().error("show does not exists");
    } else {
      return switch (currentState().process(reserveSeat)) {
        case Or.Left(var error) -> errorEffect(error, reserveSeat);
        case Or.Right(var event) -> persistEffect(event);
      };
    }
  }

  public Effect<Response> cancelReservation(CancelSeatReservation cancelSeatReservation) {
    if (currentState() == null) {
      return effects().error("show does not exists");
    } else {
      return switch (currentState().process(cancelSeatReservation)) {
        case Or.Left(var error) -> errorEffect(error, cancelSeatReservation, e -> e == DUPLICATED_COMMAND
          || e == CANCELLING_CONFIRMED_RESERVATION
          || e == RESERVATION_NOT_FOUND);
        case Or.Right(var event) -> persistEffect(event);
      };
    }
  }

  public Effect<Response> confirmPayment(ConfirmReservationPayment confirmReservationPayment) {
    if (currentState() == null) {
      return effects().error("show does not exists");
    } else {
      return switch (currentState().process(confirmReservationPayment)) {
        case Or.Left(var error) -> errorEffect(error, confirmReservationPayment);
        case Or.Right(var event) -> persistEffect(event);
      };
    }
  }

  public Effect<ShowResponse> get() {
    if (currentState() == null) {
      return effects().error("show does not exists");
    } else {
      return effects().reply(ShowResponse.from(currentState()));
    }
  }

  public Effect<SeatStatus> getSeatStatus(int seatNumber) {
    if (currentState() == null) {
      return effects().error("show does not exists");
    } else {
      return currentState().seats().get(seatNumber).fold(
        () -> effects().error("seat does not exists"),
        seat -> effects().reply(seat.status())
      );
    }
  }

  public Effect<BigDecimal> getPrice(int seatNumber) {
    if (currentState() == null) {
      return effects().error("show does not exists");
    } else {
      return currentState().seats().get(seatNumber).fold(
        () -> effects().error("seat does not exists"),
        seat -> effects().reply(seat.price())
      );
    }
  }

  private Effect<Response> persistEffect(ShowEvent showEvent) {
    return effects()
      .persist(showEvent)
      .thenReply(__ -> Success.of("ok"));
  }

  private Effect<Response> errorEffect(ShowCommandError error, ShowCommand showCommand) {
    return errorEffect(error, showCommand, e -> e == DUPLICATED_COMMAND);
  }

  private Effect<Response> errorEffect(ShowCommandError error, ShowCommand showCommand, Predicate<ShowCommandError> shouldBeSuccessful) {
    if (shouldBeSuccessful.test(error)) {
      return effects().reply(Success.of("ok"));
    } else {
      logger.error("processing command {} failed with {}", showCommand, error);
      return effects().reply(Failure.of(error.name()));
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
