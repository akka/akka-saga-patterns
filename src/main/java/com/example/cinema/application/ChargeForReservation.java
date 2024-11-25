package com.example.cinema.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import com.example.cinema.domain.ShowEvent.SeatReserved;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("charge-for-reservation")
@Consume.FromEventSourcedEntity(value = ShowEntity.class, ignoreUnknown = true)
public class ChargeForReservation extends Consumer {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ComponentClient componentClient;

  public ChargeForReservation(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect charge(SeatReserved seatReserved) {
    logger.info("charging for reservation, triggered by {}", seatReserved);
    String reservationId = seatReserved.reservationId();
    var chargeWallet = new ChargeWallet(seatReserved.price(), reservationId);

    var chargeCall = componentClient.forEventSourcedEntity(seatReserved.walletId())
      .method(WalletEntity::charge)
      .invokeAsync(chargeWallet);

    return effects().asyncDone(chargeCall);
  }
}
