package com.example.cinema.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Patch;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import com.example.cinema.application.ShowEntity;
import com.example.cinema.application.ShowResponse;
import com.example.cinema.domain.SeatStatus;
import com.example.cinema.domain.ShowCommand;
import com.example.cinema.domain.ShowCommand.ReserveSeat;

import java.util.concurrent.CompletionStage;

import static akka.javasdk.http.HttpResponses.ok;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/cinema-show")
public class ShowEndpoint {

  private final ComponentClient componentClient;

  public ShowEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Post("/{showId}")
  public CompletionStage<HttpResponse> createShow(String showId, ShowCommand.CreateShow createShow) {
    return componentClient.forEventSourcedEntity(showId)
      .method(ShowEntity::create)
      .invokeAsync(createShow)
      .thenApply(__ -> ok());
  }

  @Patch("/{showId}/reserve")
  public CompletionStage<HttpResponse> reserveSeat(String showId, ReserveSeat reserveSeat) {
    return componentClient.forEventSourcedEntity(showId)
      .method(ShowEntity::reserve)
      .invokeAsync(reserveSeat)
      .thenApply(__ -> ok());
  }

  @Get("/{showId}/seat-status/{seatNumber}")
  public CompletionStage<SeatStatus> reserveSeat(String showId, int seatNumber) {
    return componentClient.forEventSourcedEntity(showId)
      .method(ShowEntity::getSeatStatus)
      .invokeAsync(seatNumber);
  }

  @Get("/{showId}")
  public CompletionStage<ShowResponse> get(String showId) {
    return componentClient.forEventSourcedEntity(showId)
      .method(ShowEntity::get)
      .invokeAsync();
  }
}