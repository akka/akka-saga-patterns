package com.example.cinema.application;

import com.example.cinema.domain.Seat;
import com.example.cinema.domain.Show;

import java.util.List;

public record ShowResponse(String id, String title, List<Seat> seats) {

  public static ShowResponse from(Show show) {
    return new ShowResponse(show.id(), show.title(), show.seats().values().asJava());
  }
}
