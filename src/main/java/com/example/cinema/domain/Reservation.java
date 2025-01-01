package com.example.cinema.domain;

import java.math.BigDecimal;

public record Reservation(String reservationId, String showId, String walletId, BigDecimal price) {
}
