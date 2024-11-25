package com.example.cinema.domain;

import java.util.ArrayList;
import java.util.List;

public record ShowByReservation(String showId, List<String> reservationIds) {

  public ShowByReservation(String showId, String reservationId) {
    this(showId, new ArrayList<>());
    reservationIds.add(reservationId);
  }

  public ShowByReservation add(String reservationId) {
    if (!reservationIds.contains(reservationId)) {
      reservationIds.add(reservationId);
    }
    return this;
  }

  public ShowByReservation remove(String reservationId) {
    reservationIds.remove(reservationId);
    return this;
  }
}