package com.example.cinema.domain;

import java.math.BigDecimal;

import static com.example.cinema.domain.SeatReservationStatus.COMPLETED;
import static com.example.cinema.domain.SeatReservationStatus.SEAT_RESERVATION_FAILED;
import static com.example.cinema.domain.SeatReservationStatus.SEAT_RESERVATION_REFUNDED;
import static com.example.cinema.domain.SeatReservationStatus.SEAT_RESERVED;
import static com.example.cinema.domain.SeatReservationStatus.STARTED;
import static com.example.cinema.domain.SeatReservationStatus.WALLET_CHARGED;
import static com.example.cinema.domain.SeatReservationStatus.WALLET_CHARGE_REJECTED;
import static com.example.cinema.domain.SeatReservationStatus.WALLET_REFUNDED;

public record SeatReservation(String reservationId, String showId, int seatNumber, String walletId, BigDecimal price,
                              SeatReservationStatus status) {

  public SeatReservation asSeatReservationFailed() {
    return new SeatReservation(reservationId, showId, seatNumber, walletId, price, SEAT_RESERVATION_FAILED);
  }

  public SeatReservation asSeatReserved() {
    return new SeatReservation(reservationId, showId, seatNumber, walletId, price, SEAT_RESERVED);
  }

  public SeatReservation asWalletChargeRejected() {
    return new SeatReservation(reservationId, showId, seatNumber, walletId, price, WALLET_CHARGE_REJECTED);
  }

  public SeatReservation asWalletCharged() {
    return new SeatReservation(reservationId, showId, seatNumber, walletId, price, WALLET_CHARGED);
  }

  public SeatReservation asCompleted() {
    return new SeatReservation(reservationId, showId, seatNumber, walletId, price, COMPLETED);
  }

  public SeatReservation asSeatReservationRefunded() {
    return new SeatReservation(reservationId, showId, seatNumber, walletId, price, SEAT_RESERVATION_REFUNDED);
  }

  public SeatReservation asWalletRefunded() {
    return new SeatReservation(reservationId, showId, seatNumber, walletId, price, WALLET_REFUNDED);
  }

  public SeatReservation asFailed() {
    if (status == WALLET_CHARGE_REJECTED || status == STARTED) {
      return asSeatReservationFailed();
    } else if (status == WALLET_REFUNDED) {
      return asSeatReservationRefunded();
    } else {
      throw new IllegalStateException("not supported failed state transition from: " + status);
    }
  }
}
