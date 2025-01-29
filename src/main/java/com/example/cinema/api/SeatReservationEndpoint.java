package com.example.cinema.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import com.example.cinema.application.SeatReservationWorkflow;
import com.example.cinema.application.ShowEntity;
import com.example.cinema.domain.SeatReservation;

import java.math.BigDecimal;
import java.util.concurrent.CompletionStage;

import static akka.javasdk.http.HttpResponses.ok;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/seat-reservation")
public class SeatReservationEndpoint {

  private final ComponentClient componentClient;

  public SeatReservationEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public record ReserveSeatRequest(String showId, int seatNumber, String walletId) {
  }

  @Post("/{reservationId}")
  public CompletionStage<HttpResponse> reserve(String reservationId, ReserveSeatRequest reserveSeat) {
    return getPrice(reserveSeat)
      .thenCompose(price ->
        componentClient.forWorkflow(reservationId)
          .method(SeatReservationWorkflow::start)
          .invokeAsync(new SeatReservationWorkflow.ReserveSeat(reserveSeat.showId(), reserveSeat.seatNumber(), price, reserveSeat.walletId()))
          .thenApply(__ -> ok())
      );
  }

  private CompletionStage<BigDecimal> getPrice(ReserveSeatRequest reserveSeat) {
    return componentClient.forEventSourcedEntity(reserveSeat.showId())
      .method(ShowEntity::getPrice)
      .invokeAsync(reserveSeat.seatNumber());
  }

  @Get("/{reservationId}")
  public CompletionStage<SeatReservation> get(String reservationId) {
    return componentClient.forWorkflow(reservationId)
      .method(SeatReservationWorkflow::getState)
      .invokeAsync()
      .toCompletableFuture();
  }
}