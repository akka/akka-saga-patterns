package com.example.wallet.api;

import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.Metadata;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Patch;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.AbstractHttpEndpoint;
import akka.javasdk.http.HttpResponses;
import akka.stream.Materializer;
import com.example.common.Response;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.application.WalletResponse;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import com.example.wallet.domain.WalletCommand.CreateWallet;
import com.example.wallet.domain.WalletCommand.DepositFunds;

import java.math.BigDecimal;
import java.util.concurrent.CompletionStage;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/wallet")
public class WalletEndpoint extends AbstractHttpEndpoint {

  private final ComponentClient componentClient;
  private final Materializer materializer;

  public WalletEndpoint(ComponentClient componentClient, Materializer materializer) {
    this.componentClient = componentClient;
    this.materializer = materializer;
  }

  public record DepositRequest(int amount, String commandId) {
  }

  public record ChargeRequest(int amount, String expenseId, String commandId) {
  }

  @Post("/{id}/create/{amount}")
  public CompletionStage<HttpResponse> create(String id, int amount) {
    return componentClient.forEventSourcedEntity(id)
      .method(WalletEntity::create)
      .invokeAsync(new CreateWallet(id, BigDecimal.valueOf(amount)))
      .thenApply(r -> mapToHttpResponse(r, HttpResponses.created()));
  }

  @Patch("/{id}/deposit")
  public CompletionStage<HttpResponse> deposit(String id, DepositRequest depositRequest) {
    return componentClient.forEventSourcedEntity(id)
      .method(WalletEntity::deposit)
      .invokeAsync(new DepositFunds(BigDecimal.valueOf(depositRequest.amount), depositRequest.commandId))
      .thenApply(this::mapToHttpResponse);
  }

  @Patch("/{id}/charge")
  public CompletionStage<HttpResponse> charge(String id, ChargeRequest chargeRequest) {

    var metadata = requestContext().requestHeader("skip-failure-simulation")
      .map(HttpHeader::value)
      .map(skipFailureSimulation -> Metadata.EMPTY.add("skip-failure-simulation", skipFailureSimulation))
      .orElse(Metadata.EMPTY);

    return componentClient.forEventSourcedEntity(id)
      .method(WalletEntity::charge)
      .withMetadata(metadata)
      .invokeAsync(new ChargeWallet(BigDecimal.valueOf(chargeRequest.amount), chargeRequest.expenseId, chargeRequest.commandId))
      .thenApply(this::mapToHttpResponse);
  }

  @Get("/{id}")
  public CompletionStage<WalletResponse> get(String id) {
    return componentClient.forEventSourcedEntity(id)
      .method(WalletEntity::get)
      .invokeAsync();
  }

  private HttpResponse mapToHttpResponse(Response response, HttpResponse successResponse) {
    return switch (response) {
      case Response.Success(var msg) -> successResponse;
      case Response.Failure(var error) -> HttpResponses.badRequest(error);
    };
  }

  private HttpResponse mapToHttpResponse(Response response) {
    return mapToHttpResponse(response, HttpResponses.ok());
  }
}
