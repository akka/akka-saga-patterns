package com.example.wallet.domain;

import com.example.common.Or;
import com.example.common.VavrMapDeserializer;
import com.example.common.VavrMapSerializer;
import com.example.wallet.domain.WalletCommand.ChargeWallet;
import com.example.wallet.domain.WalletCommand.CreateWallet;
import com.example.wallet.domain.WalletCommand.DepositFunds;
import com.example.wallet.domain.WalletCommand.Refund;
import com.example.wallet.domain.WalletCommand.RequiresDeduplicationCommand;
import com.example.wallet.domain.WalletEvent.FundsDeposited;
import com.example.wallet.domain.WalletEvent.WalletChargeRejected;
import com.example.wallet.domain.WalletEvent.WalletCharged;
import com.example.wallet.domain.WalletEvent.WalletCreated;
import com.example.wallet.domain.WalletEvent.WalletRefunded;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.function.Supplier;

import static com.example.common.Or.left;
import static com.example.common.Or.right;
import static com.example.wallet.domain.WalletCommandError.DEPOSIT_LE_ZERO;
import static com.example.wallet.domain.WalletCommandError.EXPENSE_NOT_FOUND;
import static com.example.wallet.domain.WalletCommandError.WALLET_ALREADY_EXISTS;
import static com.example.wallet.domain.WalletCommandError.WALLET_NOT_FOUND;

public record Wallet(String id, BigDecimal balance,
                     @JsonSerialize(using = VavrMapSerializer.class)
                     @JsonDeserialize(using = VavrMapDeserializer.class)
                     Map<String, Expense> expenses,
                     LinkedHashSet<String> commandIds) {


  public static final int COMMAND_IDS_MAX_SIZE = 1000;
  public static final Wallet EMPTY = new Wallet("", BigDecimal.ZERO);

  public Wallet(String id, BigDecimal balance) {
    this(id, balance, HashMap.empty(), new LinkedHashSet<>());
  }

//  // Custom constructor for deserialization
//  @JsonCreator
//  Wallet(@JsonProperty("id") String id, @JsonProperty("balance") BigDecimal balance, @JsonProperty("expense") java.util.Map<String, Expense> expenses, @JsonProperty("commandIds") LinkedHashSet<String> commandIds) {
//    this(id, balance, Option.of(expenses).map(HashMap::ofAll).getOrElse(HashMap.empty()), commandIds);
//  }
//
//  // Custom getter for serialization
//  @JsonGetter
//  java.util.Map<String, Expense> getExpenses() {
//    return expenses.toJavaMap();
//  }

  @JsonIgnore
  public boolean isEmpty() {
    return id.equals(EMPTY.id);
  }

  public Or<WalletCommandError, WalletEvent> process(WalletCommand command) {
    if (isDuplicate(command)) {
      return left(WalletCommandError.DUPLICATED_COMMAND);
    } else {
      return switch (command) {
        case CreateWallet create -> handleCreate(create);
        case ChargeWallet charge -> ifExists(() -> handleCharge(charge));
        case DepositFunds deposit -> ifExists(() -> handleDeposit(deposit));
        case Refund refund -> ifExists(() -> handleRefund(refund));
      };
    }
  }

  private boolean isDuplicate(WalletCommand command) {
    if (command instanceof RequiresDeduplicationCommand c) {
      return commandIds.contains(c.commandId());
    } else {
      return false;
    }
  }

  private Or<WalletCommandError, WalletEvent> ifExists(Supplier<Or<WalletCommandError, WalletEvent>> processingResultSupplier) {
    if (isEmpty()) {
      return left(WALLET_NOT_FOUND);
    } else {
      return processingResultSupplier.get();
    }
  }

  private Or<WalletCommandError, WalletEvent> handleCreate(CreateWallet createWallet) {
    if (isEmpty()) {
      return right(new WalletCreated(createWallet.walletId(), createWallet.initialBalance()));
    } else {
      return left(WALLET_ALREADY_EXISTS);
    }
  }

  private Or<WalletCommandError, WalletEvent> handleCharge(ChargeWallet charge) {
    if (balance.compareTo(charge.amount()) < 0) {
      return right(new WalletChargeRejected(id, charge.expenseId()));
    } else {
      return right(new WalletCharged(id, charge.amount(), charge.expenseId(), charge.commandId()));
    }
  }

  private Or<WalletCommandError, WalletEvent> handleDeposit(DepositFunds depositFunds) {
    if (depositFunds.amount().compareTo(BigDecimal.ZERO) <= 0) {
      return left(DEPOSIT_LE_ZERO);
    } else {
      return right(new FundsDeposited(id, depositFunds.amount(), depositFunds.commandId()));
    }
  }

  private Or<WalletCommandError, WalletEvent> handleRefund(Refund refund) {
    return expenses.get(refund.expenseId()).fold(
      () -> left(EXPENSE_NOT_FOUND),
      expense -> right(new WalletRefunded(id, expense.amount(), expense.expenseId(), refund.commandId()))
    );
  }

  public Wallet apply(WalletEvent event) {
    return switch (event) {
      case WalletCreated created ->
        new Wallet(created.walletId(), created.initialBalance(), expenses, new LinkedHashSet<>());
      case WalletCharged charged -> {
        Expense expense = new Expense(charged.expenseId(), charged.amount());
        yield new Wallet(id, balance.subtract(charged.amount()), expenses.put(expense.expenseId(), expense), addCommandId(charged.commandId()));
      }
      case FundsDeposited deposited ->
        new Wallet(id, balance.add(deposited.amount()), expenses, addCommandId(deposited.commandId()));
      case WalletChargeRejected __ -> this;
      case WalletRefunded refunded ->
        new Wallet(id, balance.add(refunded.amount()), expenses.remove(refunded.expenseId()), addCommandId(refunded.commandId()));
    };
  }

  private LinkedHashSet<String> addCommandId(String commandId) {
    // To avoid infinite growth of the list with limit the size to 1000.
    // This implementation is not very efficient, so you might want to use a more dedicated data structure for it.
    // When using other collections, make sure that the state is serializable and deserializable.
    // Another way to put some constraints on the list size is to remove commandIds based on time
    // e.g. remove commandIds that are older than 1 hour.
    if (commandIds.size() >= COMMAND_IDS_MAX_SIZE) {
      commandIds.removeFirst();
    }
    commandIds.add(commandId);
    return commandIds;
  }
}
