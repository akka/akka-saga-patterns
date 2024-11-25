package com.example.cinema.domain;


import java.io.Serializable;
import java.util.List;

public record InitialShow(String id, String title, List<Seat> seats) implements Serializable {
}
