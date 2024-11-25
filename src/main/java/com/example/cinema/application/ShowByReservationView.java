package com.example.cinema.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import com.example.cinema.domain.ShowByReservation;
import com.example.cinema.domain.ShowEvent;
import com.example.cinema.domain.ShowEvent.SeatReservationCancelled;
import com.example.cinema.domain.ShowEvent.SeatReservationPaid;
import com.example.cinema.domain.ShowEvent.SeatReserved;
import com.example.cinema.domain.ShowEvent.ShowCreated;

import java.util.ArrayList;
import java.util.List;

@ComponentId("show-by-reservation-view")
public class ShowByReservationView extends View {

  @Query("SELECT * FROM show_by_reservation WHERE :reservationId = ANY(reservationIds)")
  public QueryEffect<ShowByReservation> getShow(String reservationId) {
    return queryResult();
  }

  @Consume.FromEventSourcedEntity(value = ShowEntity.class)
  public static class ShowByReservationUpdater extends TableUpdater<ShowByReservation> {

    public Effect<ShowByReservation> onEvent(ShowEvent showEvent) {
      return switch (showEvent) {
        case ShowCreated created -> effects().updateRow(new ShowByReservation(created.showId(), new ArrayList<>()));
        case SeatReserved reserved -> effects().updateRow(rowState().add(reserved.reservationId()));
        case SeatReservationPaid paid -> effects().updateRow(rowState().remove(paid.reservationId()));
        case SeatReservationCancelled cancelled -> effects().updateRow(rowState().remove(cancelled.reservationId()));
      };
    }
  }
}
