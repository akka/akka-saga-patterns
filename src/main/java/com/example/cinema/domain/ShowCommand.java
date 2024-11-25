package com.example.cinema.domain;

public sealed interface ShowCommand {

  record CreateShow(String title, int maxSeats) implements ShowCommand {
  }

  record ReserveSeat(String walletId, String reservationId, int seatNumber) implements ShowCommand {
  }

  record ConfirmReservationPayment(String reservationId) implements ShowCommand {
  }

  record CancelSeatReservation(String reservationId) implements ShowCommand {
  }
}
