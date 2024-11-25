package com.example.common;

public sealed interface Or<E, T> {

  default T rightValue() {
    if (this instanceof Left<E, T>(E error)) {
      throw new IllegalStateException("Expected right but got left: " + error);
    }
    return ((Right<E, T>) this).value();
  }

  default E leftValue() {
    if (this instanceof Right<E, T>(T value)) {
      throw new IllegalStateException("Expected left but got right: " + value);
    }
    return ((Left<E, T>) this).error();
  }

  record Left<E, T>(E error) implements Or<E, T> {
  }

  record Right<E, T>(T value) implements Or<E, T> {
  }

  static <E, T> Left<E, T> left(E error) {
    return new Left<>(error);
  }

  static <E, T> Right<E, T> right(T value) {
    return new Right<>(value);
  }

}
