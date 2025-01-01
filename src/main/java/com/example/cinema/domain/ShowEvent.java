package com.example.cinema.domain;


import akka.javasdk.annotations.TypeName;

import java.math.BigDecimal;

public sealed interface ShowEvent {
  String showId();

  @TypeName("show-created")
  record ShowCreated(String showId, InitialShow initialShow) implements ShowEvent {
  }

  @TypeName("seat-reserved")
  record SeatReserved(String showId, String walletId, String reservationId, int seatNumber,
                      BigDecimal price) implements ShowEvent {
  }

  @TypeName("seat-reservation-paid")
  record SeatReservationPaid(String showId, String reservationId, int seatNumber) implements ShowEvent {
  }

  @TypeName("seat-reservation-cancelled")
  record SeatReservationCancelled(String showId, String reservationId, int seatNumber) implements ShowEvent {
  }

  @TypeName("cancelled-reservation-confirmed")
  record CancelledReservationConfirmed(String showId, String reservationId, int seatNumber) implements ShowEvent {
  }
}
