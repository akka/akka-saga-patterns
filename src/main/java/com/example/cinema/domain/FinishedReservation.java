package com.example.cinema.domain;

public record FinishedReservation(String reservationId, ReservationStatus status) {
  public boolean isConfirmed() {
    return status == ReservationStatus.CONFIRMED;
  }

  public boolean isCancelled() {
    return status == ReservationStatus.CANCELLED;
  }
}
