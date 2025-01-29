package com.example;

import akka.javasdk.ServiceSetup;
import akka.javasdk.annotations.Setup;
import com.example.cinema.api.SeatReservationEndpoint;
import com.example.cinema.application.ChargeForReservation;
import com.example.cinema.application.CompleteReservation;
import com.example.cinema.application.HandleWalletFailures;
import com.example.cinema.application.RefundForReservation;
import com.example.cinema.application.SeatReservationWorkflow;
import com.example.cinema.application.WalletFailureEntity;
import com.typesafe.config.Config;

import java.util.Set;

@Setup
public class AppSetup implements ServiceSetup {

  private final Config config;

  public AppSetup(Config config) {
    this.config = config;
  }

  @Override
  public Set<Class<?>> disabledComponents() {
    if (config.getString("application.mode").equals("choreography")) {
      return Set.of(SeatReservationWorkflow.class, SeatReservationEndpoint.class);
    } else if (config.getString("application.mode").equals("orchestration")) {
      return Set.of(ChargeForReservation.class, CompleteReservation.class, HandleWalletFailures.class, RefundForReservation.class, WalletFailureEntity.class);
    } else {
      throw new RuntimeException("Unknown profile: " + config.getString("application.mode"));
    }
  }
}
