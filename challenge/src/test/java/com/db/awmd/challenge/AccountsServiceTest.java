package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transfer;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.NotEnoughFundsException;
import com.db.awmd.challenge.service.AccountsService;

import java.math.BigDecimal;
import java.util.UUID;

import com.db.awmd.challenge.service.NotificationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  public void makeTransfer_should_fail_when_accountDoesNotExist() {
    final String accountFromId = UUID.randomUUID().toString();
    final String accountToId = UUID.randomUUID().toString();
    this.accountsService.createAccount(new Account(accountFromId));
    Transfer transfer = new Transfer(accountFromId, accountToId, new BigDecimal(100));
    try {
      this.accountsService.makeTransfer(transfer);
      fail("Should have failed because account does not exist");
    } catch (AccountNotFoundException anfe) {
      assertThat(anfe.getMessage()).isEqualTo("Account " + accountToId + " not found.");
    }
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void makeTransfer_should_fail_when_accountNotEnoughFunds() {
    final String accountFromId = UUID.randomUUID().toString();
    final String accountToId = UUID.randomUUID().toString();
    this.accountsService.createAccount(new Account(accountFromId));
    this.accountsService.createAccount(new Account(accountToId));
    Transfer transfer = new Transfer(accountFromId, accountToId, new BigDecimal(100));
    try {
      this.accountsService.makeTransfer(transfer);
      fail("Should have failed because account does not have enough funds for the transfer");
    } catch (NotEnoughFundsException nbe) {
      assertThat(nbe.getMessage()).isEqualTo("Not enough funds on account " + accountFromId + " balance=0");
    }
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void makeTransfer_should_transferFunds() {
    final String accountFromId = UUID.randomUUID().toString();
    final String accountToId = UUID.randomUUID().toString();
    final Account accountFrom = new Account(accountFromId, new BigDecimal("500.99"));
    final Account accountTo = new Account(accountToId, new BigDecimal("20.00"));

    this.accountsService.createAccount(accountFrom);
    this.accountsService.createAccount(accountTo);

    Transfer transfer = new Transfer(accountFromId, accountToId, new BigDecimal("200.99"));

    this.accountsService.makeTransfer(transfer);

    assertThat(this.accountsService.getAccount(accountFromId).getBalance()).isEqualTo(new BigDecimal("300.00"));
    assertThat(this.accountsService.getAccount(accountToId).getBalance()).isEqualTo(new BigDecimal("220.99"));

    verifyNotifications(accountFrom, accountTo, transfer);
  }

  @Test
  public void makeTransfer_should_transferFunds_when_balanceJustEnough() {

    final String accountFromId = UUID.randomUUID().toString();
    final String accountToId = UUID.randomUUID().toString();
    final Account accountFrom = new Account(accountFromId, new BigDecimal("100.01"));
    final Account accountTo = new Account(accountToId, new BigDecimal("20.00"));

    this.accountsService.createAccount(accountFrom);
    this.accountsService.createAccount(accountTo);

    Transfer transfer = new Transfer(accountFromId, accountToId, new BigDecimal("100.01"));

    this.accountsService.makeTransfer(transfer);

    assertThat(this.accountsService.getAccount(accountFromId).getBalance()).isEqualTo(new BigDecimal("0.00"));
    assertThat(this.accountsService.getAccount(accountToId).getBalance()).isEqualTo(new BigDecimal("120.01"));

    verifyNotifications(accountFrom, accountTo, transfer);
  }

  private void verifyNotifications(final Account accountFrom, final Account accountTo, final Transfer transfer) {
    verify(notificationService, Mockito.times(1)).notifyAboutTransfer(accountFrom, "The transfer to the account with ID " + accountTo.getAccountId() + " is now complete for the amount of " + transfer.getAmount() + ".");
    verify(notificationService, Mockito.times(1)).notifyAboutTransfer(accountTo, "The account with ID + " + accountFrom.getAccountId() + " has transferred " + transfer.getAmount() + " into your account.");
  }

}
