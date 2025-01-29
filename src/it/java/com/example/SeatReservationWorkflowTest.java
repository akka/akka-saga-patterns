package com.example;

import akka.javasdk.testkit.TestKit;
import akka.javasdk.testkit.TestKitSupport;
import com.example.cinema.api.SeatReservationEndpoint.ReserveSeatRequest;
import com.example.cinema.application.ShowEntity;
import com.example.cinema.domain.SeatReservation;
import com.example.cinema.domain.SeatStatus;
import com.example.cinema.domain.ShowCommand;
import com.example.wallet.api.WalletEndpoint.ChargeRequest;
import com.example.wallet.application.WalletEntity;
import com.example.wallet.application.WalletResponse;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.model.StatusCodes.CREATED;
import static akka.http.javadsl.model.StatusCodes.OK;
import static com.example.ShowSeatReservationIntegrationTest.randomId;
import static com.example.cinema.domain.SeatReservationStatus.COMPLETED;
import static com.example.cinema.domain.SeatReservationStatus.SEAT_RESERVATION_FAILED;
import static com.example.cinema.domain.SeatReservationStatus.SEAT_RESERVATION_REFUNDED;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;


class SeatReservationWorkflowTest extends TestKitSupport {

  private Duration timeout = Duration.ofSeconds(10);

  @Override
  protected TestKit.Settings testKitSettings() {
    Config excludeChoreographyComponents = ConfigFactory.parseString("""
      application.mode = "orchestration"
      """);
    return super.testKitSettings()
      .withAdditionalConfig(excludeChoreographyComponents);
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

    ReserveSeatRequest reserveSeat = new ReserveSeatRequest(showId, seatNumber, walletId);

    //when
    reserveSeat(reservationId, reserveSeat);

    //then
    Awaitility.await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        SeatReservation seatReservation = getSeatReservation(reservationId);
        assertThat(seatReservation.status()).isEqualTo(COMPLETED);

        WalletResponse walletResponse = getWallet(walletId);
        assertThat(walletResponse.balance()).isEqualTo(new BigDecimal(200 - 100));

        SeatStatus seatStatus = getSeatStatus(showId, seatNumber);
        assertThat(seatStatus).isEqualTo(SeatStatus.PAID);
      });
  }

  private SeatStatus getSeatStatus(String showId, int seatNumber) {
    return await(componentClient.forEventSourcedEntity(showId)
      .method(ShowEntity::getSeatStatus)
      .invokeAsync(seatNumber));
  }

  private SeatReservation getSeatReservation(String reservationId) {
    return await(httpClient.GET("/seat-reservation/" + reservationId)
      .responseBodyAs(SeatReservation.class)
      .invokeAsync())
      .body();
  }

  @Test
  public void shouldRejectReservationIfCaseOfInsufficientWalletBalance() {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = randomId();
    var seatNumber = 10;

    createWallet(walletId, 50);
    createShow(showId, "pulp fiction");

    ReserveSeatRequest reserveSeat = new ReserveSeatRequest(showId, seatNumber, walletId);

    //when
    reserveSeat(reservationId, reserveSeat);

    //then
    Awaitility.await()
      .atMost(10, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .untilAsserted(() -> {
        SeatReservation seatReservation = getSeatReservation(reservationId);
        assertThat(seatReservation.status()).isEqualTo(SEAT_RESERVATION_FAILED);

        WalletResponse walletResponse = getWallet(walletId);
        assertThat(walletResponse.balance()).isEqualTo(new BigDecimal(50));

        SeatStatus seatStatus = getSeatStatus(showId, seatNumber);
        assertThat(seatStatus).isEqualTo(SeatStatus.AVAILABLE);
      });
  }

  @Test
  public void shouldCancelReservationInCaseOfWalletTimeoutAndRefundMoney() {
    //given
    var walletId = randomId();
    var showId = randomId();
    var reservationId = "42";
    var seatNumber = 10;

    createWallet(walletId, 200);
    createShow(showId, "pulp fiction");

    ReserveSeatRequest reserveSeat = new ReserveSeatRequest(showId, seatNumber, walletId);

    //when
    reserveSeat(reservationId, reserveSeat);

    //simulating charging after timeout
    chargeWallet(walletId, new ChargeRequest(100, reservationId, randomId()));

    //then
    Awaitility.await()
      .atMost(30, TimeUnit.of(SECONDS))
      .ignoreExceptions()
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        SeatReservation seatReservation = getSeatReservation(reservationId);
        assertThat(seatReservation.status()).isEqualTo(SEAT_RESERVATION_REFUNDED);

        WalletResponse walletResponse = getWallet(walletId);
        assertThat(walletResponse.balance()).isEqualTo(new BigDecimal(200));

        SeatStatus seatStatus = getSeatStatus(showId, seatNumber);
        assertThat(seatStatus).isEqualTo(SeatStatus.AVAILABLE);
      });
  }

  private void reserveSeat(String reservationId, ReserveSeatRequest reserveSeat) {
    var resposne = await(httpClient.POST("/seat-reservation/" + reservationId)
      .withRequestBody(reserveSeat)
      .invokeAsync());
    assertThat(resposne.status()).isEqualTo(OK);
  }


  private void createWallet(String walletId, int amount) {
    var resposne = await(httpClient.POST("/wallet/" + walletId + "/create/" + amount).invokeAsync());
    assertThat(resposne.status()).isEqualTo(CREATED);
  }

  private void chargeWallet(String walletId, ChargeRequest chargeRequest) {
    var resposne = await(httpClient.PATCH("/wallet/" + walletId + "/charge")
      .addHeader("skip-failure-simulation", "true")
      .withRequestBody(chargeRequest)
      .invokeAsync());
    assertThat(resposne.status()).isEqualTo(OK);
  }

  private void createShow(String showId, String title) {
    int maxSeats = 100;

    await(httpClient.POST("/cinema-show/" + showId)
      .withRequestBody(new ShowCommand.CreateShow(title, maxSeats))
      .invokeAsync()
    );
  }

  private WalletResponse getWallet(String walletId) {
    return await(componentClient.forEventSourcedEntity(walletId)
      .method(WalletEntity::get)
      .invokeAsync());
  }

}