package com.example.cinema.domain;


import com.example.cinema.domain.ShowCommand.CancelSeatReservation;
import com.example.cinema.domain.ShowCommand.CreateShow;
import com.example.cinema.domain.ShowCommand.ReserveSeat;

import static com.example.cinema.domain.DomainGenerators.randomReservationId;
import static com.example.cinema.domain.DomainGenerators.randomSeatNumber;
import static com.example.cinema.domain.DomainGenerators.randomTitle;
import static com.example.cinema.domain.DomainGenerators.randomWalletId;

public class ShowCommandGenerators {

  public static CreateShow randomCreateShow() {
    return new CreateShow(randomTitle(), ShowBuilder.MAX_SEATS);
  }

  public static ReserveSeat randomReserveSeat() {
    return new ReserveSeat(randomWalletId(), randomReservationId(), randomSeatNumber());
  }

  public static CancelSeatReservation randomCancelSeatReservation() {
    return new CancelSeatReservation(randomReservationId());
  }
}
