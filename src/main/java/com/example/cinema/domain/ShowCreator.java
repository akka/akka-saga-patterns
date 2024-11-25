package com.example.cinema.domain;

import com.example.cinema.domain.ShowCommand.CreateShow;
import com.example.cinema.domain.ShowEvent.ShowCreated;
import com.example.common.Or;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static com.example.cinema.domain.SeatStatus.AVAILABLE;
import static com.example.cinema.domain.ShowCommandError.TOO_MANY_SEATS;
import static com.example.common.Or.left;
import static com.example.common.Or.right;

public class ShowCreator {

  public static final BigDecimal INITIAL_PRICE = new BigDecimal("100");

  public static Or<ShowCommandError, ShowCreated> create(String showId, CreateShow createShow) {
    //more domain validation here
    if (createShow.maxSeats() > 100) {
      return left(TOO_MANY_SEATS);
    } else {
      var initialShow = new InitialShow(showId, createShow.title(), createSeats(INITIAL_PRICE, createShow.maxSeats()));
      var showCreated = new ShowCreated(showId, initialShow);
      return right(showCreated);
    }
  }

  public static List<Seat> createSeats(BigDecimal seatPrice, int maxSeats) {
    return IntStream.range(0, maxSeats).mapToObj(seatNum -> new Seat(seatNum, AVAILABLE, seatPrice)).toList();
  }
}
