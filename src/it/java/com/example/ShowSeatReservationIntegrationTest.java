package com.example;

import akka.javasdk.Metadata;
import akka.javasdk.http.StrictResponse;
import akka.javasdk.testkit.TestKit;
import akka.javasdk.testkit.TestKitSupport;
import akka.util.ByteString;
import com.example.cinema.domain.SeatStatus;
import com.example.cinema.domain.ShowCommand;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.application.WalletResponse;
import com.example.wallet.domain.WalletCommand;
import com.typesafe.config.ConfigFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.model.StatusCodes.CREATED;
import static akka.http.javadsl.model.StatusCodes.OK;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;


@Disabled
public class ShowSeatReservationIntegrationTest extends TestKitSupport {

  @Override
  protected TestKit.Settings testKitSettings() {
    return super.testKitSettings()
      .withAdditionalConfig(ConfigFactory.parseString("""
        application.mode = "choreography"
        """));
  }

  @Test
  public void shouldCompleteSeatReservation() {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = randomId();
    var seatNumber = 10;

    createWallet(walletId, 200);
    createShow(showId, "pulp fiction");

    //when
    var reservationResponse = reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse.status()).isEqualTo(OK);

    //then
    Awaitility.await()
      .atMost(10, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        var seatStatus = getSeatStatus(showId, seatNumber);
        assertThat(seatStatus.body()).isEqualTo(SeatStatus.PAID);
      });
  }

  @Test
  public void shouldRejectReservationIfCaseOfInsufficientWalletBalance() {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = randomId();
    var seatNumber = 11;

    createWallet(walletId, 1);
    createShow(showId, "pulp fiction 1");

    //when
    var reservationResponse = reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse.status()).isEqualTo(OK);

    //then
    Awaitility.await()
      .atMost(10, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        var seatStatus = getSeatStatus(showId, seatNumber);
        assertThat(seatStatus.body()).isEqualTo(SeatStatus.AVAILABLE);
      });
  }

  @Test
  public void shouldConfirmCancelledReservationAndRefund() {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = "42";
    var seatNumber = 11;

    createWallet(walletId, 300);
    createShow(showId, "pulp fiction");

    //when
    var reservationResponse = reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse.status()).isEqualTo(OK);

    //then
    Awaitility.await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        var seatStatus = getSeatStatus(showId, seatNumber);
        assertThat(seatStatus.body()).isEqualTo(SeatStatus.AVAILABLE);
      });

    //simulating that the wallet was actually charged
    chargeWallet(walletId, new WalletCommand.ChargeWallet(new BigDecimal(100), reservationId, randomId()));

    Awaitility.await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        WalletResponse wallet = getWallet(walletId);
        assertThat(wallet.balance()).isEqualTo(new BigDecimal(300));
      });
  }

  @Test
  public void shouldAllowToCancelAlreadyCancelledReservation() {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = "42";
    var seatNumber = 11;

    createWallet(walletId, 300);
    createShow(showId, "pulp fiction");

    //when
    var reservationResponse = reserveSeat(showId, walletId, reservationId, seatNumber);
    assertThat(reservationResponse.status()).isEqualTo(OK);

    //then
    Awaitility.await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        var seatStatus = getSeatStatus(showId, seatNumber);
        assertThat(seatStatus.body()).isEqualTo(SeatStatus.AVAILABLE);
      });

    //simulating that the wallet charging was rejected for this reservation
    chargeWallet(walletId, new WalletCommand.ChargeWallet(new BigDecimal(400), reservationId, randomId()));

    Awaitility.await()
      .atMost(20, TimeUnit.of(SECONDS))
      .untilAsserted(() -> {
        WalletResponse wallet = getWallet(walletId);
        assertThat(wallet.balance()).isEqualTo(new BigDecimal(300));
      });
  }

  private WalletResponse getWallet(String walletId) {
    return await(componentClient.forEventSourcedEntity(walletId)
      .method(WalletEntity::get)
      .invokeAsync());
  }

  private void chargeWallet(String walletId, WalletCommand.ChargeWallet chargeWallet) {
    await(componentClient.forEventSourcedEntity(walletId)
      .method(WalletEntity::charge)
      .withMetadata(Metadata.EMPTY.add("skip-failure-simulation", "true"))
      .invokeAsync(chargeWallet));
  }

  static String randomId() {
    return UUID.randomUUID().toString().substring(0, 7);
  }

  private StrictResponse<ByteString> reserveSeat(String showId, String walletId, String reservationId, int seatNumber) {
    return await(httpClient.PATCH("/cinema-show/" + showId + "/reserve")
      .withRequestBody(new ShowCommand.ReserveSeat(walletId, reservationId, seatNumber))
      .invokeAsync());
  }

  private StrictResponse<SeatStatus> getSeatStatus(String showId, int seatNumber) {
    return await(httpClient.GET("/cinema-show/" + showId + "/seat-status/" + seatNumber)
      .responseBodyAs(SeatStatus.class)
      .invokeAsync());
  }

  private void createWallet(String walletId, int amount) {
    var resposne = await(httpClient.POST("/wallet/" + walletId + "/create/" + amount).invokeAsync());
    assertThat(resposne.status()).isEqualTo(CREATED);
  }

  private void createShow(String showId, String title) {
    int maxSeats = 100;

    await(httpClient.POST("/cinema-show/" + showId)
      .withRequestBody(new ShowCommand.CreateShow(title, maxSeats))
      .invokeAsync()
    );
  }
}