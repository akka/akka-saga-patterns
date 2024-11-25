package com.example.cinema.domain;

import com.example.cinema.domain.ShowCommand.CancelSeatReservation;
import com.example.cinema.domain.ShowCommand.ConfirmReservationPayment;
import com.example.cinema.domain.ShowCommand.CreateShow;
import com.example.cinema.domain.ShowCommand.ReserveSeat;
import com.example.cinema.domain.ShowEvent.SeatReservationCancelled;
import com.example.cinema.domain.ShowEvent.SeatReservationPaid;
import com.example.cinema.domain.ShowEvent.SeatReserved;
import com.example.cinema.domain.ShowEvent.ShowCreated;
import com.example.common.Or;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.util.List;
import java.util.Optional;

import static com.example.cinema.domain.ShowCommandError.RESERVATION_NOT_FOUND;
import static com.example.cinema.domain.ShowCommandError.SEAT_NOT_AVAILABLE;
import static com.example.cinema.domain.ShowCommandError.SEAT_NOT_EXISTS;
import static com.example.cinema.domain.ShowCommandError.SHOW_ALREADY_EXISTS;
import static com.example.common.Or.left;
import static com.example.common.Or.right;

public record Show(String id, String title, Map<Integer, Seat> seats, Map<String, Integer> pendingReservations) {

  public static Show create(ShowCreated showCreated) {
    InitialShow initialShow = showCreated.initialShow();
    List<Tuple2<Integer, Seat>> seats = initialShow.seats().stream().map(seat -> new Tuple2<>(seat.number(), seat)).toList();
    return new Show(initialShow.id(), initialShow.title(), HashMap.ofEntries(seats), HashMap.empty());
  }

  public Or<ShowCommandError, ShowEvent> process(ShowCommand command) {
    return switch (command) {
      case CreateShow ignored -> left(SHOW_ALREADY_EXISTS);
      case ReserveSeat reserveSeat -> handleReservation(reserveSeat);
      case ConfirmReservationPayment confirmReservationPayment -> handleConfirmation(confirmReservationPayment);
      case CancelSeatReservation cancelSeatReservation -> handleCancellation(cancelSeatReservation);
    };
  }

  private Or<ShowCommandError, ShowEvent> handleConfirmation(ConfirmReservationPayment confirmReservationPayment) {
    return pendingReservations.get(confirmReservationPayment.reservationId()).fold(
        () -> left(RESERVATION_NOT_FOUND),
        seatNumber -> seats.get(seatNumber)
            .<Or<ShowCommandError, ShowEvent>>map(seat ->
                right(new SeatReservationPaid(id, confirmReservationPayment.reservationId(), seatNumber))
            ).getOrElse(left(SEAT_NOT_EXISTS))
    );
  }

  private Or<ShowCommandError, ShowEvent> handleReservation(ReserveSeat reserveSeat) {
    int seatNumber = reserveSeat.seatNumber();
    return seats.get(seatNumber).<Or<ShowCommandError, ShowEvent>>map(seat -> {
      if (seat.isAvailable()) {
        return right(new SeatReserved(id, reserveSeat.walletId(), reserveSeat.reservationId(), seatNumber, seat.price()));
      } else {
        return left(SEAT_NOT_AVAILABLE);
      }
    }).getOrElse(left(SEAT_NOT_EXISTS));
  }

  private Or<ShowCommandError, ShowEvent> handleCancellation(CancelSeatReservation cancelSeatReservation) {
    return pendingReservations.get(cancelSeatReservation.reservationId()).fold(
        () -> left(RESERVATION_NOT_FOUND),
        seatNumber -> seats.get(seatNumber)
            .<Or<ShowCommandError, ShowEvent>>map(seat ->
                right(new SeatReservationCancelled(id, cancelSeatReservation.reservationId(), seatNumber))
            ).getOrElse(left(SEAT_NOT_EXISTS))
    );
  }

  public Show apply(ShowEvent event) {
    return switch (event) {
      case ShowCreated ignored -> throw new IllegalStateException("Show is already created, use Show.create instead.");
      case SeatReserved seatReserved -> applyReserved(seatReserved);
      case SeatReservationPaid seatReservationPaid -> applyReservationPaid(seatReservationPaid);
      case SeatReservationCancelled seatReservationCancelled -> applyReservationCancelled(seatReservationCancelled);
    };
  }

  private Show applyReservationPaid(SeatReservationPaid seatReservationPaid) {
    Seat seat = getSeatOrThrow(seatReservationPaid.seatNumber());
    return new Show(id, title, seats.put(seat.number(), seat.paid()), pendingReservations.remove(seatReservationPaid.reservationId()));

  }

  private Show applyReservationCancelled(SeatReservationCancelled seatReservationCancelled) {
    Seat seat = getSeatOrThrow(seatReservationCancelled.seatNumber());
    return new Show(id, title, seats.put(seat.number(), seat.available()), pendingReservations.remove(seatReservationCancelled.reservationId()));
  }

  private Show applyReserved(SeatReserved seatReserved) {
    Seat seat = getSeatOrThrow(seatReserved.seatNumber());
    return new Show(id, title, seats.put(seat.number(), seat.reserved()), pendingReservations.put(seatReserved.reservationId(), seatReserved.seatNumber()));
  }

  private Seat getSeatOrThrow(int seatNumber) {
    return seats.get(seatNumber).getOrElseThrow(() -> new IllegalStateException("Seat not exists %s".formatted(seatNumber)));
  }

  public Optional<Seat> getSeat(int seatNumber) {
    return seats.get(seatNumber).toJavaOptional();
  }
}

