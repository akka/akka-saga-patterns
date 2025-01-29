package com.example.wallet.domain;

import java.math.BigDecimal;

public record Expense(String expenseId, BigDecimal amount) {
}
