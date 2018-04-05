package com.db.awmd.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Arrays;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  @Getter
  private final NotificationService notificationService;

  @Autowired
  private TransferValidator transferValidator;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  /**
   * Makes a transfer between two accounts for the balance specified by the {@link Transfer} object
   * @param transfer
   * @throws AccountNotFoundException When an account does not exist
   * @throws NotEnoughFundsException When there are not enough funds to complete the transfer
   * @throws TransferBetweenSameAccountException Transfer to self account is not permitted
   */
  public void makeTransfer(Transfer transfer) throws AccountNotFoundException, NotEnoughFundsException, TransferBetweenSameAccountException {

    final Account accountFrom = accountsRepository.getAccount(transfer.getAccountFromId());
    final Account accountTo = accountsRepository.getAccount(transfer.getAccountToId());
    final BigDecimal amount = transfer.getAmount();

    transferValidator.validate(accountFrom, accountTo, transfer);

    //ideally atomic operation in production
    boolean successful = accountsRepository.updateAccountsBatch(Arrays.asList(
            new AccountUpdate(accountFrom.getAccountId(), amount.negate()),
            new AccountUpdate(accountTo.getAccountId(), amount)
    ));

    if (successful){
      notificationService.notifyAboutTransfer(accountFrom, "The transfer to the account with ID " + accountTo.getAccountId() + " is now complete for the amount of " + transfer.getAmount() + ".");
      notificationService.notifyAboutTransfer(accountTo, "The account with ID + " + accountFrom.getAccountId() + " has transferred " + transfer.getAmount() + " into your account.");
    }
  }

}
