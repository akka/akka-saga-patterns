package com.example.wallet.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Patch;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.application.WalletResponse;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletCommand.CreateWallet;
import com.example.wallet.domain.WalletCommand.DepositFunds;

import java.math.BigDecimal;
import java.util.concurrent.CompletionStage;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/wallet")
public class WalletEndpoint {

  private final ComponentClient componentClient;

  public WalletEndpoint(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  record DepositRequest(int amount, String commandId) {
  }

  @Post("/{id}/create/{amount}")
  public CompletionStage<HttpResponse> create(String id, int amount) {
    return componentClient.forEventSourcedEntity(id)
      .method(WalletEntity::create)
      .invokeAsync(new CreateWallet(id, BigDecimal.valueOf(amount)))
      .thenApply(__ -> HttpResponses.created());
  }

  @Patch("/{id}/deposit")
  public CompletionStage<HttpResponse> deposit(String id, DepositRequest depositRequest) {
    return componentClient.forEventSourcedEntity(id)
      .method(WalletEntity::deposit)
      .invokeAsync(new DepositFunds(BigDecimal.valueOf(depositRequest.amount), depositRequest.commandId))
      .thenApply(__ -> HttpResponses.ok());
  }

  @Get("/{id}")
  public CompletionStage<WalletResponse> get(String id) {
    return componentClient.forEventSourcedEntity(id)
      .method(WalletEntity::get)
      .invokeAsync();
  }
}
