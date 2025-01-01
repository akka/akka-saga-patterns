package com.example.cinema.domain;

public record FinishedReservation(String reservationId, int seatNumber, ReservationStatus status) {
  public boolean isConfirmed() {
    return status == ReservationStatus.CONFIRMED;
  }

  public boolean isCancelled() {
    return status == ReservationStatus.CANCELLED;
  }
}
