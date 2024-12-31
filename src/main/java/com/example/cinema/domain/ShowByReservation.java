package com.example.cinema.domain;

import java.util.HashSet;
import java.util.Set;

public record ShowByReservation(String showId, Set<String> reservationIds) {

  public ShowByReservation(String showId) {
    this(showId, new HashSet<>());
  }

  public ShowByReservation add(String reservationId) {
    reservationIds.add(reservationId);
    return this;
  }

  public ShowByReservation remove(String reservationId) {
    reservationIds.remove(reservationId);
    return this;
  }
}