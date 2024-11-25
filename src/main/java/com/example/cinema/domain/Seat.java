package com.example.cinema.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

import static com.example.cinema.domain.SeatStatus.AVAILABLE;
import static com.example.cinema.domain.SeatStatus.PAID;
import static com.example.cinema.domain.SeatStatus.RESERVED;

public record Seat(int number, SeatStatus status, BigDecimal price) {
  @JsonIgnore
  public boolean isAvailable() {
    return status == AVAILABLE;
  }

  public Seat reserved() {
    return new Seat(number, RESERVED, price);
  }

  public Seat paid() {
    return new Seat(number, PAID, price);
  }

  public Seat available() {
    return new Seat(number, AVAILABLE, price);
  }
}
