package com.example.cinema.domain;

import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.util.List;

import static com.example.cinema.domain.DomainGenerators.randomPrice;
import static com.example.cinema.domain.DomainGenerators.randomShowId;
import static com.example.cinema.domain.ShowCreator.createSeats;

public class ShowBuilder {

  final static int MAX_SEATS = 100;
  private String id = randomShowId();
  private String title = "Random title";
  private Map<Integer, Seat> seats = HashMap.empty();
  private Map<String, Integer> pendingReservations = HashMap.empty();

  public static ShowBuilder showBuilder() {
    return new ShowBuilder();
  }

  public ShowBuilder withRandomSeats() {
    List<Tuple2<Integer, Seat>> seatTuples = createSeats(randomPrice(), MAX_SEATS)
      .stream().map(seat -> new Tuple2<>(seat.number(), seat)).toList();
    this.seats = HashMap.ofEntries(seatTuples);
    return this;
  }

  public ShowBuilder withSeatReservation(Seat seat, String reservationId) {
    seats = seats.put(seat.number(), seat);
    pendingReservations = pendingReservations.put(reservationId, seat.number());
    return this;
  }

  public Show build() {
    return new Show(id, title, seats, pendingReservations, HashMap.empty());
  }


}
